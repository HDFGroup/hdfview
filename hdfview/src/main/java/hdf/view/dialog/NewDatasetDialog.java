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

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.ScalarDS;
import hdf.view.DataView.DataView;
import hdf.view.ImageView.ImageView;
import hdf.view.TableView.TableView;
import hdf.view.Tools;
import hdf.view.ViewProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * NewDatasetDialog shows a message dialog requesting user input for creating a
 * new HDF4/5 dataset.
 *
 * @author Jordan T. Henderson
 * @version 2.4 12/31/2015
 */
public class NewDatasetDialog extends NewDataObjectDialog {

    private static final Logger log = LoggerFactory.getLogger(NewDatasetDialog.class);

    private String maxSize;

    private Text currentSizeField;
    private Text chunkSizeField;
    private Text fillValueField;

    private Combo parentChoice;
    private Combo rankChoice;
    private Combo compressionLevel;

    private Button checkCompression;
    private Button checkFillValue;

    private Button checkContiguous;
    private Button checkChunked;

    /** TextField for entering the name of the object. */
    protected Text nameField;

    /** A list of current groups. */
    private List<Group> groupList;

    private final DataView dataView;

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
    public NewDatasetDialog(Shell parent, Group pGroup, List<?> objs)
    {
        super(parent, pGroup, objs);

        dataView = null;
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
    public NewDatasetDialog(Shell parent, Group pGroup, List<?> objs, DataView observer)
    {
        super(parent, pGroup, objs);

        dataView = observer;
    }

    /**
     * Open the NewDataseteDialog for adding a new dataset.
     */
    public void open()
    {
        Shell parent = getParent();
        shell        = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("New Dataset...");
        shell.setImages(ViewProperties.getHdfIcons());
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
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                parentObj = groupList.get(parentChoice.getSelectionIndex());
            }
        });

        groupList            = new Vector<>();
        Object obj           = null;
        Iterator<?> iterator = objList.iterator();

        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                Group g = (Group)obj;
                groupList.add(g);
                if (g.isRoot()) {
                    parentChoice.add(HObject.SEPARATOR);
                }
                else {
                    parentChoice.add(g.getPath() + g.getName() + HObject.SEPARATOR);
                }
            }
        }

        if (((Group)parentObj).isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.SEPARATOR));
        }
        else {
            parentChoice.select(
                parentChoice.indexOf(parentObj.getPath() + parentObj.getName() + HObject.SEPARATOR));
        }

        // Create New Dataset from scratch
        if (dataView == null) {
            // Create Datatype region
            createDatatypeWidget();

            // Create Dataspace region
            org.eclipse.swt.widgets.Group dataspaceGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
            dataspaceGroup.setFont(curFont);
            dataspaceGroup.setText("Dataspace");
            dataspaceGroup.setLayout(new GridLayout(3, true));
            dataspaceGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            Label label = new Label(dataspaceGroup, SWT.LEFT);
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
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    int rank                     = rankChoice.getSelectionIndex() + 1;
                    StringBuilder currentSizeStr = new StringBuilder("1");

                    for (int i = 1; i < rank; i++) {
                        currentSizeStr.append(" x 1");
                    }

                    currentSizeField.setText(currentSizeStr.toString());

                    String currentStr = currentSizeField.getText();
                    int idx           = currentStr.lastIndexOf('x');

                    StringBuilder chunkStr = new StringBuilder();

                    if (rank <= 1) {
                        chunkStr.append(currentStr);
                    }
                    else {
                        chunkStr.append("1");
                        for (int i = 1; i < rank - 1; i++) {
                            chunkStr.append(" x 1");
                        }
                        if (idx > 0) {
                            chunkStr.append(" x ");
                            chunkStr.append(currentStr.substring(idx + 1));
                        }
                    }

                    chunkSizeField.setText(chunkStr.toString());
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
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    if (maxSize == null || maxSize.length() < 1)
                        maxSize = currentSizeField.getText();

                    String msg = new InputDialog(shell, "Set Max Size",
                                                 "Enter max dimension sizes. \n"
                                                     + "Use \"unlimited\" for unlimited dimension size.\n\n"
                                                     + "For example,\n"
                                                     + "    200 x 100\n"
                                                     + "    100 x unlimited\n\n",
                                                 maxSize)
                                     .open();

                    if (msg == null || msg.length() < 1)
                        maxSize = currentSizeField.getText();
                    else
                        maxSize = msg;

                    checkMaxSize();
                }
            });

            // Create Storage Properties region
            org.eclipse.swt.widgets.Group storagePropertiesGroup =
                new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
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
                @Override
                public void widgetSelected(SelectionEvent e)
                {
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
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    chunkSizeField.setEnabled(true);
                    StringBuilder chunkStr = new StringBuilder();
                    StringTokenizer st     = new StringTokenizer(currentSizeField.getText(), "x");
                    int rank               = rankChoice.getSelectionIndex() + 1;
                    while (st.hasMoreTokens()) {
                        long l = Math.max(1, Long.valueOf(st.nextToken().trim()) / (2 * rank));
                        chunkStr.append(String.valueOf(l));
                        chunkStr.append("x");
                    }
                    String chunkString = chunkStr.substring(0, chunkStr.lastIndexOf("x"));
                    chunkSizeField.setText(chunkString);
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
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    boolean isCompressed = checkCompression.getSelection();

                    if (isCompressed && isH5) {
                        if (!checkChunked.getSelection()) {
                            int rank               = rankChoice.getSelectionIndex() + 1;
                            String currentStr      = currentSizeField.getText();
                            int idx                = currentStr.lastIndexOf('x');
                            StringBuilder chunkStr = new StringBuilder();

                            if (rank <= 1) {
                                chunkStr.append(currentStr);
                            }
                            else {
                                chunkStr.append("1");
                                for (int i = 1; i < rank - 1; i++) {
                                    chunkStr.append(" x 1");
                                }
                                if (idx > 0) {
                                    chunkStr.append(" x ");
                                    chunkStr.append(currentStr.substring(idx + 1));
                                }
                            }

                            chunkSizeField.setText(chunkStr.toString());
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

            if (isH5) {
                checkFillValue = new Button(storagePropertiesGroup, SWT.CHECK);
                checkFillValue.setFont(curFont);
                checkFillValue.setText("Fill Value");
                checkFillValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
                checkFillValue.setSelection(false);
                checkFillValue.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        fillValueField.setEnabled(checkFillValue.getSelection());
                    }
                });

                fillValueField = new Text(storagePropertiesGroup, SWT.SINGLE | SWT.BORDER);
                fillValueField.setFont(curFont);
                fillValueField.setText("0");
                fillValueField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
                fillValueField.setEnabled(false);
            }
            else {
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
            @Override
            public void widgetSelected(SelectionEvent e)
            {
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
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                newObject = null;
                shell.dispose();
                ((Vector<Group>)groupList).setSize(0);
            }
        });

        if (dataView == null) {
            Button helpButton = new Button(buttonComposite, SWT.PUSH);
            helpButton.setFont(curFont);
            helpButton.setText(" &Help ");
            helpButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
            helpButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    new HelpDialog(shell).open();
                }
            });
        }

        shell.pack();

        shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (curFont != null)
                    curFont.dispose();
            }
        });

        shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Rectangle parentBounds = parent.getBounds();
        Point shellSize        = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                          (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

        shell.open();

        Display display = shell.getDisplay();
        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();
    }

    /** Check if the max size is valid. */
    private void checkMaxSize()
    {
        boolean isChunkNeeded = false;
        String dimStr         = currentSizeField.getText();
        String maxSizeStr     = maxSize;
        StringTokenizer stMax = new StringTokenizer(maxSizeStr, "x");
        StringTokenizer stDim = new StringTokenizer(dimStr, "x");

        if (stMax.countTokens() != stDim.countTokens()) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Check", "Wrong number of values in the max dimension size " + maxSize);
            maxSize = null;
            return;
        }

        int rank       = stDim.countTokens();
        long max       = 0;
        long dim       = 0;
        long[] maxdims = new long[rank];
        for (int i = 0; i < rank; i++) {
            String token = stMax.nextToken().trim();

            token = token.toLowerCase();
            if (token.startsWith("u")) {
                max           = -1;
                isChunkNeeded = true;
            }
            else {
                try {
                    max = Long.parseLong(token);
                }
                catch (NumberFormatException ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Check", "Invalid max dimension size: " + maxSize);
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
                Tools.showError(shell, "Check", "Invalid dimension size: " + dimStr);
                return;
            }

            if (max != -1 && max < dim) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Check", "Invalid max dimension size: " + maxSize);
                maxSize = null;
                return;
            }
            else if (max > dim) {
                isChunkNeeded = true;
            }

            maxdims[i] = max;
        } //  (int i = 0; i < rank; i++)

        if (isH5) {
            if (isChunkNeeded && !checkChunked.getSelection()) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Check", "Chunking is required for the max dimensions of " + maxSize);
                checkChunked.setSelection(true);
            }
        }
        else {
            for (int i = 1; i < rank; i++) {
                if (maxdims[i] <= 0) {
                    maxSize = currentSizeField.getText();
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Check", "Only dim[0] can be unlimited." + maxSize);
                    return;
                }
            }
        }
    }

    private HObject createFromScratch()
    {
        String name  = null;
        Group pgroup = null;
        int rank     = -1;
        int gzip     = -1;
        long[] dims;
        long[] maxdims = null;
        long[] chunks  = null;

        name = nameField.getText().trim();
        if ((name == null) || (name.length() < 1)) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Dataset name is not specified.");
            return null;
        }

        if (name.indexOf(HObject.SEPARATOR) >= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Dataset name cannot contain path.");
            return null;
        }

        pgroup = groupList.get(parentChoice.getSelectionIndex());

        if (pgroup == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Parent group is null.");
            return null;
        }

        rank               = rankChoice.getSelectionIndex() + 1;
        StringTokenizer st = new StringTokenizer(currentSizeField.getText(), "x");
        if (st.countTokens() < rank) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create",
                            "Number of values in the current dimension size is less than " + rank);
            return null;
        }

        long l       = 0;
        dims         = new long[rank];
        String token = null;
        for (int i = 0; i < rank; i++) {
            token = st.nextToken().trim();
            try {
                l = Long.parseLong(token);
            }
            catch (NumberFormatException ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Invalid dimension size: " + currentSizeField.getText());
                return null;
            }

            if (l <= 0) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Dimension size must be greater than zero.");
                return null;
            }

            dims[i] = l;
        }

        String maxSizeStr = maxSize;
        if (maxSizeStr != null && maxSizeStr.length() > 1) {
            st = new StringTokenizer(maxSizeStr, "x");
            if (st.countTokens() < rank) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create",
                                "Number of values in the max dimension size is less than " + rank);
                return null;
            }

            l       = 0;
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
                        Tools.showError(shell, "Create", "Invalid max dimension size: " + maxSize);
                        return null;
                    }
                }

                if (l < -1) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Create", "Dimension size cannot be less than -1.");
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
                Tools.showError(shell, "Create", "Number of values in the chunk size is less than " + rank);
                return null;
            }

            l      = 0;
            chunks = new long[rank];
            for (int i = 0; i < rank; i++) {
                token = st.nextToken().trim();
                try {
                    l = Long.parseLong(token);
                }
                catch (NumberFormatException ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Create",
                                    "Invalid chunk dimension size: " + chunkSizeField.getText());
                    return null;
                }

                if (l < 1) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Create", "Chunk size cannot be less than 1.");
                    return null;
                }

                chunks[i] = l;
            } //  (int i=0; i<rank; i++)

            long tchunksize = 1;
            long tdimsize   = 1;
            for (int i = 0; i < rank; i++) {
                tchunksize *= chunks[i];
                tdimsize *= dims[i];
            }

            if (tchunksize >= tdimsize) {
                shell.getDisplay().beep();
                if (!Tools.showConfirm(shell, "Create",
                                       "Chunk size is equal/greater than the current size. "
                                           + "\nAre you sure you want to set chunk size to " +
                                           chunkSizeField.getText() + "?")) {
                    return null;
                }
            }

            if (tchunksize == 1) {
                shell.getDisplay().beep();
                if (!Tools.showConfirm(
                        shell, "Create",
                        "Chunk size is one, which may cause large memory overhead for large dataset."
                            + "\nAre you sure you want to set chunk size to " + chunkSizeField.getText() +
                            "?")) {
                    return null;
                }
            }
        } //  (checkChunked.isSelected())

        if (checkCompression.getSelection()) {
            gzip = compressionLevel.getSelectionIndex();
        }
        else {
            gzip = 0;
        }

        HObject obj = null;
        try {
            Datatype datatype = createNewDatatype(null);

            String fillValue = null;

            if (fillValueField != null) {
                if (fillValueField.isEnabled())
                    fillValue = fillValueField.getText();
            }

            obj = fileFormat.createScalarDS(name, pgroup, datatype, dims, maxdims, chunks, gzip, fillValue,
                                            null);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", ex.getMessage());
            return null;
        }

        return obj;
    }

    private HObject createFromTable()
    {
        HObject obj = null;

        String name  = null;
        Group pgroup = null;

        name = nameField.getText();
        if (name == null || name.length() == 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Dataset name is not specified.");
            return null;
        }

        if (name.indexOf(HObject.SEPARATOR) >= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Dataset name cannot contain path.");
            return null;
        }

        pgroup = groupList.get(parentChoice.getSelectionIndex());
        if (pgroup == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Parent group is null.");
            return null;
        }

        TableView tableView = (TableView)dataView;
        Object theData      = tableView.getSelectedData();
        if (theData == null) {
            return null;
        }

        int w           = tableView.getSelectedColumnCount();
        int h           = tableView.getSelectedRowCount();
        Dataset dataset = (Dataset)tableView.getDataObject();
        if (dataset instanceof ScalarDS) {
            ScalarDS sd = (ScalarDS)dataset;
            if (sd.getDatatype().isUnsigned()) {
                theData = Dataset.convertToUnsignedC(theData, null);
            }
        }

        try {
            long[] dims = {h, w};
            obj         = dataset.copy(pgroup, name, dims, theData);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", ex.getMessage());
            return null;
        }

        return obj;
    }

    private HObject createFromImage()
    {
        HObject obj  = null;
        String name  = null;
        Group pgroup = null;

        name = nameField.getText();
        if (name == null || name.length() == 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Dataset name is not specified.");
            return null;
        }

        if (name.indexOf(HObject.SEPARATOR) >= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Dataset name cannot contain path.");
            return null;
        }

        pgroup = groupList.get(parentChoice.getSelectionIndex());
        if (pgroup == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Parent group is null.");
            return null;
        }

        ImageView imageView = (ImageView)dataView;
        ScalarDS dataset    = (ScalarDS)imageView.getDataObject();
        Object theData      = imageView.getSelectedData();

        if (theData == null) {
            return null;
        }

        // in version 2.4, unsigned image data is converted to signed data
        // to write data, the data needs to be converted back to unsigned.
        if (dataset.getDatatype().isUnsigned()) {
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
                    dims    = new long[2];
                    dims[0] = h;
                    dims[1] = w;
                }
            }
            else {
                dims    = new long[2];
                dims[0] = w;
                dims[1] = h;
            }

            obj = dataset.copy(pgroup, name, dims, theData);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", ex.getMessage());
            return null;
        }

        return obj;
    }

    private class HelpDialog extends Dialog {
        private Shell helpShell;

        private static final String HELP_INFORMATION = """
                How to Create a New Dataset

                The following instructions explain how to create a new dataset.
                This dialog allows the creation of a dataset (an HDF4 SDS or an
                HDF5 dataset). The dataset can be a 1 to 32 dimension array of
                numbers, characters, or strings.

                To create a dataset, it is necessary to define its name, parent
                group, datatype, and dataspace (i.e., the dimensions).
                Optionally, the storage properties can be specified.

                The dataset will be created and filled with zeros. Data can be
                added with the hdfedit tool, or by a program.

                1) Dataset name and path
                The name of the new dataset must follow the HDF5 name rules
                (similar to the Unix name rules). The name may contain almost
                any characters, but it must not contain the path separator, '/'.

                The dataset must be a member of some Group. The 'Parent group'
                selection lists all the Groups in the file.

                2) Datatype
                The datatype specifies the type of the data elements of the
                array. This Java-based tool supports the datatypes: integer,
                float, character, string, reference, enum, variable-length
                integer, variable-length float, and variable-length string.

                The size specifies the size of a single data point in bits, such
                as 32-bit integer or 64-bit float. The size of a float is either
                32-bit or 64-bit.

                For HDF5, there are three byte order choices: NATIVE, LITTLE
                ENDIAN and BIG ENDIAN. "NATIVE" byte order means that the byte
                order is determined by the machine. The byte order cannot be
                specified for HDF4.

                3) Dataspace
                The dataspace specifies the number of dimensions (rank), current
                dimension size and maximum dimension size. The dimension size is
                separated by "x". For example, a 3D dataset might show the
                dimensions as: 20 x 30 x 5.

                The current size must be greater than zero, and the maximum size
                must be at least as large as the current size. A maximum size of
                zero means the maximum size will be set to the current size.
                Setting the maximum size to -1 will make the dimension
                "unlimited".

                4) Storage layout and data compression
                There are two options for storage layout: contiguous or chunked.
                The default storage layout is contiguous. If chunked layout is
                selected, the chunk size must be specified.

                The dataset may be compressed with GZIP. The compression level
                ranges from zero (no compression) to 9 (highest compression). In
                HDF5, if compression is selected then the dataset must be
                chunked.
                """;

        HelpDialog(Shell parent)
        {
            super(parent, SWT.APPLICATION_MODAL);
        }

        public void open()
        {
            Shell parent = getParent();
            helpShell =
                new Shell(parent, SWT.TITLE | SWT.CLOSE | SWT.RESIZE | SWT.BORDER | SWT.APPLICATION_MODAL);
            helpShell.setFont(curFont);
            helpShell.setText("Create New Dataset");
            helpShell.setImages(ViewProperties.getHdfIcons());
            helpShell.setLayout(new GridLayout(1, true));

            // Render the help text with a native StyledText widget
            StyledText helpText =
                new StyledText(helpShell, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
            helpText.setFont(curFont);
            helpText.setText(HELP_INFORMATION);
            helpText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            Button okButton = new Button(helpShell, SWT.PUSH);
            okButton.setFont(curFont);
            okButton.setText("   &OK   ");
            okButton.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
            okButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    helpShell.dispose();
                }
            });

            helpShell.pack();

            helpShell.setSize(new Point(500, 500));

            Rectangle parentBounds = parent.getBounds();
            Point shellSize        = helpShell.getSize();
            helpShell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                                  (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));

            helpShell.open();

            Display display = parent.getDisplay();
            while (!helpShell.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
        }
    }
}
