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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import hdf.object.CompoundDS;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.Group;
import hdf.object.HObject;

import hdf.object.h5.H5CompoundDS;

import hdf.view.Tools;
import hdf.view.ViewProperties;

/**
 * NewCompoundDatasetDialog shows a message dialog requesting user input for creating
 * a new HDF4/5 compound dataset.
 *
 * @author Jordan T. Henderson
 * @version 2.4 1/7/2015
 */
public class NewCompoundDatasetDialog extends NewDataObjectDialog {

    private static final Logger log = LoggerFactory.getLogger(NewCompoundDatasetDialog.class);

    private static final String[] DATATYPE_NAMES   = {
        "byte (8-bit)", // 0
        "short (16-bit)", // 1
        "int (32-bit)", // 2
        "unsigned byte (8-bit)", // 3
        "unsigned short (16-bit)", // 4
        "unsigned int (32-bit)", // 5
        "long (64-bit)", // 6
        "float", // 7
        "double", // 8
        "string", // 9
        "enum", // 10
        "unsigned long (64-bit)" // 11
    };

    private Combo                 parentChoice, nFieldBox, templateChoice;

    /** A list of current groups */
    private Vector<Group>         groupList;
    private Vector<CompoundDS>    compoundDSList;

    private int                   numberOfMembers;

    private Table                 table;

    private TableEditor[][]       editors;

    private Text                  nameField, currentSizeField, maxSizeField, chunkSizeField;

    private Combo                 compressionLevel, rankChoice;
    private Button                checkCompression;
    private Button                checkContiguous, checkChunked;

    /**
     * Constructs a NewCompoundDatasetDialog with specified list of possible parent
     * groups.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
    public NewCompoundDatasetDialog(Shell parent, Group pGroup, List<?> objs) {
        super(parent, pGroup, objs);

        numberOfMembers = 2;

        groupList = new Vector<>(objs.size());
        compoundDSList = new Vector<>(objs.size());
    }

    /**
     * Open the NewCompoundDatasetDialog for adding a new compound dataset.
     */
    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("New Compound Dataset...");
        shell.setImages(ViewProperties.getHdfIcons());
        shell.setLayout(new GridLayout(1, false));


        // Create Name/Parent Group/Import field region
        Composite fieldComposite = new Composite(shell, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.verticalSpacing = 0;
        fieldComposite.setLayout(layout);
        fieldComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Label label = new Label(fieldComposite, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Dataset name: ");

        nameField = new Text(fieldComposite, SWT.SINGLE | SWT.BORDER);
        nameField.setFont(curFont);
        nameField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        label = new Label(fieldComposite, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Parent group: ");

        parentChoice = new Combo(fieldComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        parentChoice.setFont(curFont);
        parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        parentChoice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                parentObj = groupList.get(parentChoice.getSelectionIndex());
            }
        });

        Object obj = null;
        Iterator<?> iterator = objList.iterator();

        while (iterator.hasNext()) {
            obj = iterator.next();
            if (obj instanceof Group) {
                Group g = (Group) obj;
                groupList.add(g);
                if (g.isRoot()) {
                    parentChoice.add(HObject.SEPARATOR);
                }
                else {
                    parentChoice.add(g.getPath() + g.getName() + HObject.SEPARATOR);
                }
            }
            else if (obj instanceof CompoundDS) {
                compoundDSList.add((CompoundDS) obj);
            }
        }

        if (((Group) parentObj).isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.SEPARATOR));
        }
        else {
            parentChoice.select(parentChoice.indexOf(parentObj.getPath() + parentObj.getName() + HObject.SEPARATOR));
        }

        label = new Label(fieldComposite, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Import template: ");

        templateChoice = new Combo(fieldComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        templateChoice.setFont(curFont);
        templateChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        templateChoice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                CompoundDS dset = null;
                String name = templateChoice.getItem(templateChoice.getSelectionIndex());

                log.trace("templateChoice start name={}", name);
                for (CompoundDS ds : compoundDSList)
                    if (ds.getName().equals(name))
                        dset = ds;

                if (dset == null) return;

                if (!dset.isInited())
                    dset.init();

                int rank = dset.getRank();
                rankChoice.select(rank - 1);
                long[] dims = dset.getDims();
                final String[] mNames = dset.getMemberNames();
                int[] mOrders = dset.getMemberOrders();
                Datatype[] mTypes = dset.getMemberTypes();

                String sizeStr = String.valueOf(dims[0]);
                for (int i = 1; i < rank; i++) {
                    sizeStr += "x" + dims[i];
                }
                currentSizeField.setText(sizeStr);

                try {
                    ((H5CompoundDS)dset).getMetadata();
                } // get chunking and compression info
                catch (Exception ex) {
                    log.debug("get chunking and compression info:", ex);
                }
                long[] chunks = dset.getChunkSize();
                if (chunks != null) {
                    checkChunked.setSelection(true);
                    sizeStr = String.valueOf(chunks[0]);
                    for (int i = 1; i < rank; i++) {
                        sizeStr += "x" + chunks[i];
                    }
                    chunkSizeField.setText(sizeStr);
                }

                String compression = dset.getCompression();
                if (compression != null) {
                    int clevel = -1;
                    int comp_pos = Dataset.COMPRESSION_GZIP_TXT.length();
                    int idx = compression.indexOf(Dataset.COMPRESSION_GZIP_TXT);
                    if (idx >= 0) {
                        try {
                            clevel = Integer.parseInt(compression.substring(idx + comp_pos, idx + comp_pos +1));
                        }
                        catch (NumberFormatException ex) {
                            clevel = -1;
                        }
                    }
                    if (clevel > 0) {
                        checkCompression.setSelection(true);
                        compressionLevel.select(clevel);
                    }
                }

                nFieldBox.select(dset.getMemberCount() - 1);
                nFieldBox.notifyListeners(SWT.Selection, new Event());
                for (int i = 0; i < numberOfMembers; i++) {
                    ((Text) editors[i][0].getEditor()).setText(mNames[i]);

                    log.trace("mNames[{}] = {}", i, mNames[i]);
                    int typeIdx = -1;
                    int tclass = mTypes[i].getDatatypeClass();
                    long tsize = mTypes[i].getDatatypeSize();
                    int tsigned = mTypes[i].getDatatypeSign();
                    if (tclass == Datatype.CLASS_ARRAY) {
                        tclass = mTypes[i].getDatatypeBase().getDatatypeClass();
                        tsize = mTypes[i].getDatatypeBase().getDatatypeSize();
                        tsigned = mTypes[i].getDatatypeBase().getDatatypeSign();
                    }
                    if (tclass == Datatype.CLASS_CHAR) {
                        if (tsigned == Datatype.SIGN_NONE) {
                            if (tsize == 1) {
                                typeIdx = 3;
                            }
                        }
                        else {
                            if (tsize == 1) {
                                typeIdx = 0;
                            }
                        }
                    }
                    if (tclass == Datatype.CLASS_INTEGER) {
                        if (tsigned == Datatype.SIGN_NONE) {
                            if (tsize == 1) {
                                typeIdx = 3;
                            }
                            else if (tsize == 2) {
                                typeIdx = 4;
                            }
                            else if (tsize == 4) {
                                typeIdx = 5;
                            }
                            else {
                                typeIdx = 11;
                            }
                        }
                        else {
                            if (tsize == 1) {
                                typeIdx = 0;
                            }
                            else if (tsize == 2) {
                                typeIdx = 1;
                            }
                            else if (tsize == 4) {
                                typeIdx = 2;
                            }
                            else {
                                typeIdx = 6;
                            }
                        }
                    }
                    else if (tclass == Datatype.CLASS_FLOAT) {
                        if (tsize == 4) {
                            typeIdx = 7;
                        }
                        else {
                            typeIdx = 8;
                        }
                    }
                    else if (tclass == Datatype.CLASS_STRING) {
                        typeIdx = 9;
                    }
                    else if (tclass == Datatype.CLASS_ENUM) {
                        typeIdx = 10;
                    }
                    if (typeIdx < 0) {
                        continue;
                    }
                    log.trace("typeIdx={}", typeIdx);

                    CCombo typeCombo = ((CCombo) editors[i][1].getEditor());
                    typeCombo.select(typeIdx);
                    typeCombo.notifyListeners(SWT.Selection, new Event());

                    //TODO: Array size is wrong for enums and for array types. Array types such as 8x8
                    // show as size 64, not 8x8
                    if (tclass == Datatype.CLASS_STRING) {
                        ((Text) editors[i][2].getEditor()).setText(String.valueOf(tsize));
                    }
                    else if (tclass == Datatype.CLASS_ENUM) {
                        ((Text) editors[i][2].getEditor()).setText(mTypes[i].getEnumMembersAsString());
                        table.getItem(i).setText(2, mTypes[i].getEnumMembersAsString());
                    }
                    else {
                        ((Text) editors[i][2].getEditor()).setText(String.valueOf(mOrders[i]));
                    }
                } //  (int i=0; i<numberOfMembers; i++)
            }
        });

        Iterator<CompoundDS> it = compoundDSList.iterator();
        while(it.hasNext()) {
            templateChoice.add(it.next().getName());
        }


        // Create Dataspace region
        org.eclipse.swt.widgets.Group dataspaceGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        dataspaceGroup.setLayout(new GridLayout(3, true));
        dataspaceGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        dataspaceGroup.setFont(curFont);
        dataspaceGroup.setText("Dataspace");

        label = new Label(dataspaceGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("No. of dimensions");

        label = new Label(dataspaceGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Current size");

        label = new Label(dataspaceGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Max size (-1 for unlimited)");

        rankChoice = new Combo(dataspaceGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        rankChoice.setFont(curFont);
        rankChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        rankChoice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int rank = rankChoice.getSelectionIndex() + 1;
                String currentSizeStr = "1";
                String maxSizeStr = "0";

                for (int i = 1; i < rank; i++) {
                    currentSizeStr += " x 1";
                    maxSizeStr += " x 0";
                }

                currentSizeField.setText(currentSizeStr);
                maxSizeField.setText(maxSizeStr);

                String currentStr = currentSizeField.getText();
                int idx = currentStr.lastIndexOf('x');
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

        currentSizeField = new Text(dataspaceGroup, SWT.SINGLE | SWT.BORDER);
        currentSizeField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        currentSizeField.setFont(curFont);
        currentSizeField.setText("1");

        maxSizeField = new Text(dataspaceGroup, SWT.SINGLE | SWT.BORDER);
        maxSizeField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        maxSizeField.setFont(curFont);
        maxSizeField.setText("0");


        // Create Data Layout/Compression region
        org.eclipse.swt.widgets.Group layoutGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        layoutGroup.setLayout(new GridLayout(7, false));
        layoutGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        layoutGroup.setFont(curFont);
        layoutGroup.setText("Data Layout and Compression");

        label = new Label(layoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Storage layout: ");

        checkContiguous = new Button(layoutGroup, SWT.RADIO);
        checkContiguous.setFont(curFont);
        checkContiguous.setText("Contiguous");
        checkContiguous.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        checkContiguous.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                chunkSizeField.setEnabled(false);
            }
        });

        // Dummy labels
        label = new Label(layoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("");
        label = new Label(layoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("");

        checkChunked = new Button(layoutGroup, SWT.RADIO);
        checkChunked.setFont(curFont);
        checkChunked.setText("Chunked");
        checkChunked.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        checkChunked.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                chunkSizeField.setEnabled(true);

                String currentStr = currentSizeField.getText();
                int idx = currentStr.lastIndexOf('x');
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
        });

        label = new Label(layoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Size: ");

        chunkSizeField = new Text(layoutGroup, SWT.SINGLE | SWT.BORDER);
        chunkSizeField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        chunkSizeField.setFont(curFont);
        chunkSizeField.setText("1");
        chunkSizeField.setEnabled(false);

        label = new Label(layoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Compression: ");

        checkCompression = new Button(layoutGroup, SWT.CHECK);
        checkCompression.setFont(curFont);
        checkCompression.setText("gzip");
        checkCompression.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        checkCompression.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isCompressed = checkCompression.getSelection();

                if (isCompressed) {
                    if (!checkChunked.getSelection()) {
                        String currentStr = currentSizeField.getText();
                        int idx = currentStr.lastIndexOf('x');
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
                    compressionLevel.setEnabled(false);
                    checkContiguous.setEnabled(true);
                }
            }
        });

        label = new Label(layoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Level: ");

        compressionLevel = new Combo(layoutGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        compressionLevel.setFont(curFont);
        compressionLevel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        compressionLevel.setEnabled(false);

        for (int i = 0; i < 10; i++) {
            compressionLevel.add(String.valueOf(i));
        }

        label = new Label(layoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("");

        label = new Label(layoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("");

        label = new Label(layoutGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("");


        // Create Properties region
        org.eclipse.swt.widgets.Group propertiesGroup = new org.eclipse.swt.widgets.Group(shell, SWT.NONE);
        propertiesGroup.setLayout(new GridLayout(2, false));
        propertiesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        propertiesGroup.setFont(curFont);
        propertiesGroup.setText("Compound Datatype Properties");

        label = new Label(propertiesGroup, SWT.LEFT);
        label.setFont(curFont);
        label.setText("Number of Members:");

        nFieldBox = new Combo(propertiesGroup, SWT.DROP_DOWN);
        nFieldBox.setFont(curFont);
        nFieldBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        nFieldBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateMemberTableItems();
            }
        });
        nFieldBox.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) updateMemberTableItems();
            }
        });

        for (int i = 1; i <= 100; i++) {
            nFieldBox.add(String.valueOf(i));
        }

        table = new Table(propertiesGroup, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        table.setLinesVisible(false);
        table.setHeaderVisible(true);
        table.setFont(curFont);

        editors = new TableEditor[nFieldBox.getItemCount()][3];

        String[] colNames = { "Name", "Datatype", "Array size / String length / Enum names" };

        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(colNames[0]);

        column = new TableColumn(table, SWT.NONE);
        column.setText(colNames[1]);

        column = new TableColumn(table, SWT.NONE);
        column.setText(colNames[2]);

        for (int i = 0; i < 2; i++) {
            TableEditor[] editor = addMemberTableItem(table);
            editors[i][0] = editor[0];
            editors[i][1] = editor[1];
            editors[i][2] = editor[2];
        }

        for(int i = 0; i < table.getColumnCount(); i++) {
            table.getColumn(i).pack();
        }

        // Last table column always expands to fill remaining table size
        table.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event e) {
                Table table = (Table) e.widget;
                Rectangle area = table.getClientArea();
                int columnCount = table.getColumnCount();
                int totalGridLineWidth = (columnCount - 1) * table.getGridLineWidth();

                int totalColumnWidth = 0;
                for (TableColumn column : table.getColumns()) {
                    totalColumnWidth += column.getWidth();
                }

                int diff = area.width - (totalColumnWidth + totalGridLineWidth);

                TableColumn col = table.getColumns()[columnCount - 1];
                col.setWidth(diff + col.getWidth());
            }
        });

        // Disable table selection highlighting
        table.addListener(SWT.EraseItem, new Listener() {
            @Override
            public void handleEvent(Event e) {
                if ((e.detail & SWT.SELECTED) != 0) {
                    e.detail &= ~SWT.SELECTED;
                }
            }
        });

        // Create Ok/Cancel button region
        Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, true));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setFont(curFont);
        okButton.setText("   &OK   ");
        okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    newObject = createCompoundDS();
                }
                catch (Exception ex) {
                    Tools.showError(shell, "Create", ex.getMessage());
                }

                if (newObject != null) {
                    shell.dispose();
                }
            }
        });

        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setFont(curFont);
        cancelButton.setText(" &Cancel ");
        cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newObject = null;
                shell.dispose();
                (groupList).setSize(0);
            }
        });

        templateChoice.deselectAll();
        rankChoice.select(0);
        checkContiguous.setSelection(true);
        compressionLevel.select(6);
        nFieldBox.select(nFieldBox.indexOf(String.valueOf(numberOfMembers)));

        shell.pack();

        table.getColumn(0).setWidth(table.getClientArea().width / 3);
        table.getColumn(1).setWidth(table.getClientArea().width / 3);

        shell.addDisposeListener(new DisposeListener() {
            @Override
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

    private HObject createCompoundDS() throws Exception {
        HObject obj = null;
        long dims[], maxdims[], chunks[];
        int rank;

        maxdims = chunks = null;
        String dname = nameField.getText();
        if ((dname == null) || (dname.length() <= 0)) {
            throw new IllegalArgumentException("Dataset name is empty");
        }

        Group pgroup = groupList.get(parentChoice.getSelectionIndex());
        if (pgroup == null) {
            throw new IllegalArgumentException("Invalid parent group");
        }

        int n = table.getItemCount();
        if (n <= 0) {
            return null;
        }

        String[] mNames = new String[n];
        Datatype[] mDatatypes = new Datatype[n];
        int[] mOrders = new int[n];

        for (int i = 0; i < n; i++) {
            String name = (String) table.getItem(i).getData("MemberName");
            if ((name == null) || (name.length() <= 0)) {
                throw new IllegalArgumentException("Member name is empty");
            }
            mNames[i] = name;
            log.trace("createCompoundDS member[{}] name = {}", i, mNames[i]);

            int order = 1;
            String orderStr = (String) table.getItem(i).getData("MemberSize");
            if (orderStr != null) {
                try {
                    order = Integer.parseInt(orderStr);
                }
                catch (Exception ex) {
                    log.debug("compound order:", ex);
                }
            }
            mOrders[i] = order;

            String typeName = (String) table.getItem(i).getData("MemberType");
            log.trace("createCompoundDS type[{}] name = {}", i, typeName);
            Datatype type = null;
            if (DATATYPE_NAMES[0].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 1, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[1].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 2, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[2].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[3].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 1, Datatype.NATIVE, Datatype.SIGN_NONE);
            }
            else if (DATATYPE_NAMES[4].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 2, Datatype.NATIVE, Datatype.SIGN_NONE);
            }
            else if (DATATYPE_NAMES[5].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.SIGN_NONE);
            }
            else if (DATATYPE_NAMES[6].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 8, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[7].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[8].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[9].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_STRING, order, Datatype.NATIVE, Datatype.NATIVE);
            }
            else if (DATATYPE_NAMES[10].equals(typeName)) { // enum
                type = fileFormat.createDatatype(Datatype.CLASS_ENUM, 4, Datatype.NATIVE, Datatype.NATIVE);
                if ((orderStr == null) || (orderStr.length() < 1) || orderStr.endsWith("...")) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Create", "Invalid member values: " + orderStr);
                    return null;
                }
                else {
                    type.setEnumMembers(orderStr);
                }
            }
            else if (DATATYPE_NAMES[11].equals(typeName)) {
                type = fileFormat.createDatatype(Datatype.CLASS_INTEGER, 8, Datatype.NATIVE, Datatype.SIGN_NONE);
            }
            else {
                throw new IllegalArgumentException("Invalid data type.");
            }
            mDatatypes[i] = type;
        } //  (int i=0; i<n; i++)

        rank = rankChoice.getSelectionIndex() + 1;
        log.trace("createCompoundDS rank={}", rank);
        StringTokenizer st = new StringTokenizer(currentSizeField.getText(), "x");
        if (st.countTokens() < rank) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Number of values in the current dimension size is less than " + rank);
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

        st = new StringTokenizer(maxSizeField.getText(), "x");
        if (st.countTokens() < rank) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Number of values in the max dimension size is less than " + rank);
            return null;
        }

        l = 0;
        maxdims = new long[rank];
        for (int i = 0; i < rank; i++) {
            token = st.nextToken().trim();
            try {
                l = Long.parseLong(token);
            }
            catch (NumberFormatException ex) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Invalid max dimension size: " + maxSizeField.getText());
                return null;
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

        chunks = null;
        if (checkChunked.getSelection()) {
            st = new StringTokenizer(chunkSizeField.getText(), "x");
            if (st.countTokens() < rank) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Number of values in the chunk size is less than " + rank);
                return null;
            }

            l = 0;
            chunks = new long[rank];
            token = null;
            for (int i = 0; i < rank; i++) {
                token = st.nextToken().trim();
                try {
                    l = Long.parseLong(token);
                }
                catch (NumberFormatException ex) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Create", "Invalid chunk dimension size: " + chunkSizeField.getText());
                    return null;
                }

                if (l < 1) {
                    shell.getDisplay().beep();
                    Tools.showError(shell, "Create", "Chunk size cannot be less than 1.");
                    return null;
                }

                chunks[i] = l;
            } //  (int i=0; i<rank; i++)

            long tchunksize = 1, tdimsize = 1;
            for (int i = 0; i < rank; i++) {
                tchunksize *= chunks[i];
                tdimsize *= dims[i];
            }

            if (tchunksize >= tdimsize) {
                shell.getDisplay().beep();
                if(!Tools.showConfirm(shell, "Create", "Chunk size is equal/greater than the current size. "
                        + "\nAre you sure you want to set chunk size to " + chunkSizeField.getText() + "?")) {
                    return null;
                }
            }

            if (tchunksize == 1) {
                shell.getDisplay().beep();
                if(!Tools.showConfirm(shell, "Create", "Chunk size is one, which may cause large memory overhead for large dataset."
                        + "\nAre you sure you want to set chunk size to " + chunkSizeField.getText() + "?")) {
                    return null;
                }
            }

        } //  (checkChunked.getSelection())

        int gzip = 0;
        if (checkCompression.getSelection()) {
            gzip = compressionLevel.getSelectionIndex();
        }

        if (checkChunked.getSelection()) {
            obj = fileFormat.createCompoundDS(dname, pgroup, dims, maxdims, chunks, gzip, mNames, mDatatypes, mOrders, null);
        }
        else {
            obj = fileFormat.createCompoundDS(dname, pgroup, dims, maxdims, null, -1, mNames, mDatatypes, mOrders, null);
        }
        return obj;
    }

    private void updateMemberTableItems() {
        int n = 0;

        try {
            n = Integer.valueOf(nFieldBox.getItem(nFieldBox.getSelectionIndex())).intValue();
        }
        catch (Exception ex) {
            log.debug("Change number of members:", ex);
            return;
        }

        if (n == numberOfMembers) {
            return;
        }

        if(n > numberOfMembers) {
            try {
                for (int i = numberOfMembers; i < n; i++) {
                    TableEditor[] editor = addMemberTableItem(table);
                    editors[i][0] = editor[0];
                    editors[i][1] = editor[1];
                    editors[i][2] = editor[2];
                }
            }
            catch (Exception ex) {
                log.debug("Error adding member table items: ", ex);
                return;
            }
        } else {
            try {
                for(int i = numberOfMembers - 1; i >= n; i--) {
                    table.remove(i);
                }
            }
            catch (Exception ex) {
                log.debug("Error removing member table items: ", ex);
                return;
            }
        }

        table.setItemCount(n);
        numberOfMembers = n;
    }

    private TableEditor[] addMemberTableItem(Table table) {
        final TableItem item = new TableItem(table, SWT.NONE);
        final TableEditor[] editor = new TableEditor[3];

        for (int i = 0; i < editor.length; i++) editor[i] = new TableEditor(table);

        final Text nameText = new Text(table, SWT.SINGLE | SWT.BORDER);
        nameText.setFont(curFont);

        editor[0].grabHorizontal = true;
        editor[0].grabVertical = true;
        editor[0].horizontalAlignment = SWT.LEFT;
        editor[0].verticalAlignment = SWT.TOP;
        editor[0].setEditor(nameText, item, 0);

        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Text text = (Text) e.widget;
                item.setData("MemberName", text.getText());
            }
        });

        final CCombo typeCombo = new CCombo(table, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.setFont(curFont);
        typeCombo.setItems(DATATYPE_NAMES);

        editor[1].grabHorizontal = true;
        editor[1].grabVertical = true;
        editor[1].horizontalAlignment = SWT.LEFT;
        editor[1].verticalAlignment = SWT.TOP;
        editor[1].setEditor(typeCombo, item, 1);

        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                CCombo combo = (CCombo) e.widget;
                item.setData("MemberType", combo.getItem(combo.getSelectionIndex()));
            }
        });

        final Text sizeText = new Text(table, SWT.SINGLE | SWT.BORDER);
        sizeText.setFont(curFont);

        editor[2].grabHorizontal = true;
        editor[2].grabVertical = true;
        editor[2].horizontalAlignment = SWT.LEFT;
        editor[2].verticalAlignment = SWT.TOP;
        editor[2].setEditor(sizeText, item, 2);

        sizeText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Text text = (Text) e.widget;
                item.setData("MemberSize", text.getText());
            }
        });

        item.setData("MemberName", "");
        item.setData("MemberType", "");
        item.setData("MemberSize", "");

        item.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                editor[0].dispose();
                editor[1].dispose();
                editor[2].dispose();
                nameText.dispose();
                typeCombo.dispose();
                sizeText.dispose();
            }
        });

        return editor;
    }
}
