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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.object.Group;
import hdf.object.HObject;
import hdf.view.Tools;
import hdf.view.ViewProperties;

/**
 * NewGroupDialog shows a message dialog requesting user input for creating a new HDF4/5 group.
 *
 * @author Jordan T. Henderson
 * @version 2.4 12/30/2015
 */
public class NewGroupDialog extends NewDataObjectDialog {

    /* Used to restore original size after click "less" button */
    private Point       originalSize;

    private Text        nameField;
    private Text        compactField;
    private Text        indexedField;

    private Combo       parentChoice;
    private Combo       orderFlags;

    private Button      useCreationOrder;
    private Button      setLinkStorage;
    private Button      creationOrderHelpButton;
    private Button      storageTypeHelpButton;
    private Button      okButton;
    private Button      cancelButton;
    private Button      moreButton;

    private Composite   moreOptionsComposite;
    private Composite   creationOrderComposite;
    private Composite   storageTypeComposite;
    private Composite   dummyComposite;
    private Composite   buttonComposite;

    private List<Group> groupList;

    private int         creationOrder;

    private boolean     moreOptionsEnabled;

    /**
     * Constructs a NewGroupDialog with specified list of possible parent groups.
     *
     * @param parent
     *            the parent shell of the dialog
     * @param pGroup
     *            the parent group which the new group is added to.
     * @param objs
     *            the list of all objects.
     */
    public NewGroupDialog(Shell parent, Group pGroup, List<?> objs) {
        super(parent, pGroup, objs);

        moreOptionsEnabled = false;
    }

    /**
     * Open the NewGroupDialog for adding a new group.
     */
    public void open() {
        Shell parent = getParent();
        shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(curFont);
        shell.setText("New Group...");
        shell.setImages(ViewProperties.getHdfIcons());
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 0;
        shell.setLayout(layout);

        Composite fieldComposite = new Composite(shell, SWT.NONE);
        fieldComposite.setLayout(new GridLayout(2, false));
        fieldComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Label groupNameLabel = new Label(fieldComposite, SWT.LEFT);
        groupNameLabel.setFont(curFont);
        groupNameLabel.setText("Group name:");

        nameField = new Text(fieldComposite, SWT.SINGLE | SWT.BORDER);
        nameField.setFont(curFont);
        GridData fieldData = new GridData(SWT.FILL, SWT.FILL, true, false);
        fieldData.minimumWidth = 250;
        nameField.setLayoutData(fieldData);

        Label parentGroupLabel = new Label(fieldComposite, SWT.LEFT);
        parentGroupLabel.setFont(curFont);
        parentGroupLabel.setText("Parent group:");
        parentGroupLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        parentChoice = new Combo(fieldComposite, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
        parentChoice.setFont(curFont);
        parentChoice.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        parentChoice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                parentObj = groupList.get(parentChoice.getSelectionIndex());
            }
        });

        groupList = new ArrayList<>();
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
        }

        if (((Group) parentObj).isRoot()) {
            parentChoice.select(parentChoice.indexOf(HObject.SEPARATOR));
        }
        else {
            parentChoice.select(parentChoice.indexOf(parentObj.getPath() +
                    parentObj.getName() + HObject.SEPARATOR));
        }

        // Only add "More" button if file is H5 type
        if(isH5) {
            moreOptionsComposite = new Composite(shell, SWT.NONE);
            moreOptionsComposite.setLayout(new GridLayout(2, false));
            moreOptionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

            moreButton = new Button(moreOptionsComposite, SWT.PUSH);
            moreButton.setFont(curFont);
            moreButton.setText("   More   ");
            moreButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
            moreButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    moreOptionsEnabled = !moreOptionsEnabled;

                    if(moreOptionsEnabled) {
                        addMoreOptions();
                    }
                    else {
                        removeMoreOptions();
                    }
                }
            });

            dummyComposite = new Composite(moreOptionsComposite, SWT.NONE);
            dummyComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        }
        else {
            // Add dummy label to take up space as dialog is resized
            new Label(shell, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        }

        buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, true));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setFont(curFont);
        okButton.setText("   &OK   ");
        okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newObject = create();
                if (newObject != null) {
                    shell.dispose();
                }
            }
        });

        cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setFont(curFont);
        cancelButton.setText(" &Cancel ");
        cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newObject = null;
                shell.dispose();
            }
        });

        shell.pack();

        shell.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (curFont != null) curFont.dispose();
            }
        });

        shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        originalSize = shell.getSize();

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

    private HObject create() {
        String name = null;
        Group pgroup = null;
        long gcpl = 0;

        name = nameField.getText();
        if (name == null || name.length() == 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Group name is not specified.");
            return null;
        }

        if (name.indexOf(HObject.SEPARATOR) >= 0) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Group name cannot contain path.");
            return null;
        }

        pgroup = groupList.get(parentChoice.getSelectionIndex());

        if (pgroup == null) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", "Parent group is null.");
            return null;
        }

        Group obj = null;

        if (orderFlags != null && orderFlags.isEnabled()) {
            String order = orderFlags.getItem(orderFlags.getSelectionIndex());
            if (order.equals("Tracked"))
                creationOrder = Group.CRT_ORDER_TRACKED;
            else if (order.equals("Tracked+Indexed"))
                creationOrder = Group.CRT_ORDER_INDEXED;
        }
        else
            creationOrder = 0;

        if ((orderFlags != null) && ((orderFlags.isEnabled()) || (setLinkStorage.getSelection()))) {
            int maxCompact = Integer.parseInt(compactField.getText());
            int minDense = Integer.parseInt(indexedField.getText());

            if ((maxCompact <= 0) || (maxCompact > 65536) || (minDense > 65536)) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Max Compact and Min Indexed should be > 0 and < 65536.");
                return null;
            }

            if (maxCompact < minDense) {
                shell.getDisplay().beep();
                Tools.showError(shell, "Create", "Min Indexed should be <= Max Compact");
                return null;
            }

            try {
                gcpl = fileFormat.createGcpl(creationOrder, maxCompact, minDense);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        try {
            if (isH5)
                obj = fileFormat.createGroup(name, pgroup, 0, gcpl);
            else
                obj = fileFormat.createGroup(name, pgroup);
        }
        catch (Exception ex) {
            shell.getDisplay().beep();
            Tools.showError(shell, "Create", ex.getMessage());
            return null;
        }

        return obj;
    }

    private void addMoreOptions() {
        moreButton.setText("   Less   ");

        creationOrderComposite = new Composite(moreOptionsComposite, SWT.BORDER);
        creationOrderComposite.setLayout(new GridLayout(4, true));
        creationOrderComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        creationOrderHelpButton = new Button(creationOrderComposite, SWT.PUSH);
        creationOrderHelpButton.setImage(ViewProperties.getHelpIcon());
        creationOrderHelpButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        creationOrderHelpButton.setToolTipText("Help on Creation Order");
        creationOrderHelpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final String msg = "Use Creation Order allows the user to set the creation order \n"
                        + "of links in a group, so that tracking, indexing, and iterating over links\n"
                        + "in groups can be possible. \n\n"
                        + "If the order flag Tracked is selected, links in a group can now \n"
                        + "be explicitly tracked by the order that they were created. \n\n"
                        + "If the order flag Tracked+Indexed is selected, links in a group can \n"
                        + "now be explicitly tracked and indexed in the order that they were created. \n\n"
                        + "The default order in which links in a group are listed is alphanumeric-by-name. \n\n\n";

                Tools.showInformation(shell, "Create", msg);
            }
        });

        useCreationOrder = new Button(creationOrderComposite, SWT.CHECK);
        useCreationOrder.setFont(curFont);
        useCreationOrder.setText("Use Creation Order");
        useCreationOrder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isOrder = useCreationOrder.getSelection();

                if (isOrder)
                    orderFlags.setEnabled(true);
                else
                    orderFlags.setEnabled(false);
            }
        });

        Label label = new Label(creationOrderComposite, SWT.RIGHT);
        label.setFont(curFont);
        label.setText("Order Flags: ");
        label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        orderFlags = new Combo(creationOrderComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
        orderFlags.setFont(curFont);
        orderFlags.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        orderFlags.add("Tracked");
        orderFlags.add("Tracked+Indexed");
        orderFlags.select(orderFlags.indexOf("Tracked"));
        orderFlags.setEnabled(false);


        storageTypeComposite = new Composite(moreOptionsComposite, SWT.BORDER);
        storageTypeComposite.setLayout(new GridLayout(3, true));
        storageTypeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        storageTypeHelpButton = new Button(storageTypeComposite, SWT.PUSH);
        storageTypeHelpButton.setImage(ViewProperties.getHelpIcon());
        storageTypeHelpButton.setToolTipText("Help on set Link Storage");
        storageTypeHelpButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        storageTypeHelpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final String msg = "Set Link Storage allows the users to explicitly set the storage  \n"
                        + "type of a group to be Compact or Indexed. \n\n"
                        + "Compact Storage: For groups with only a few links, compact link storage\n"
                        + "allows groups containing only a few links to take up much less space \n" + "in the file. \n\n"
                        + "Indexed Storage: For groups with large number of links, indexed link storage  \n"
                        + "provides a faster and more scalable method for storing and working with  \n"
                        + "large groups containing many links. \n\n"
                        + "The threshold for switching between the compact and indexed storage   \n"
                        + "formats is either set to default values or can be set by the user. \n\n"
                        + "<html><b>Max Compact</b></html> \n"
                        + "Max Compact is the maximum number of links to store in the group in a  \n"
                        + "compact format, before converting the group to the Indexed format. Groups \n"
                        + "that are in compact format and in which the number of links rises above \n"
                        + " this threshold are automatically converted to indexed format. \n\n"
                        + "<html><b>Min Indexed</b></html> \n"
                        + "Min Indexed is the minimum number of links to store in the Indexed format.   \n"
                        + "Groups which are in indexed format and in which the number of links falls    \n"
                        + "below this threshold are automatically converted to compact format. \n\n\n";

                Tools.showInformation(shell, "Create", msg);
            }
        });

        setLinkStorage = new Button(storageTypeComposite, SWT.CHECK);
        setLinkStorage.setFont(curFont);
        setLinkStorage.setText("Set Link Storage");
        setLinkStorage.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (setLinkStorage.getSelection()) {
                    compactField.setEnabled(true);
                    indexedField.setEnabled(true);
                }
                else {
                    compactField.setText("8");
                    compactField.setEnabled(false);
                    indexedField.setText("6");
                    indexedField.setEnabled(false);
                }
            }
        });

        Composite indexedComposite = new Composite(storageTypeComposite, SWT.NONE);
        indexedComposite.setLayout(new GridLayout(2, true));
        indexedComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        Label minLabel = new Label(indexedComposite, SWT.LEFT);
        minLabel.setFont(curFont);
        minLabel.setText("Min Indexed: ");

        Label maxLabel = new Label(indexedComposite, SWT.LEFT);
        maxLabel.setFont(curFont);
        maxLabel.setText("Max Compact: ");

        indexedField = new Text(indexedComposite, SWT.SINGLE | SWT.BORDER);
        indexedField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        indexedField.setFont(curFont);
        indexedField.setText("6");
        indexedField.setTextLimit(5);
        indexedField.setEnabled(false);
        indexedField.addVerifyListener(new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent e) {
                String input = e.text;

                char[] chars = new char[input.length()];
                input.getChars(0, chars.length, chars, 0);
                for (int i = 0; i < chars.length; i++) {
                   if (!('0' <= chars[i] && chars[i] <= '9')) {
                      e.doit = false;
                      return;
                   }
                }
            }
        });

        compactField = new Text(indexedComposite, SWT.SINGLE | SWT.BORDER);
        compactField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        compactField.setFont(curFont);
        compactField.setText("8");
        compactField.setTextLimit(5);
        compactField.setEnabled(false);
        compactField.addVerifyListener(new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent e) {
                String input = e.text;

                char[] chars = new char[input.length()];
                input.getChars(0, chars.length, chars, 0);
                for (int i = 0; i < chars.length; i++) {
                   if (!('0' <= chars[i] && chars[i] <= '9')) {
                      e.doit = false;
                      return;
                   }
                }
            }
        });

        shell.pack();

        shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        Rectangle parentBounds = shell.getParent().getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                          (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));
    }

    private void removeMoreOptions() {
        moreButton.setText("   More   ");

        creationOrderHelpButton.dispose();
        storageTypeHelpButton.dispose();

        creationOrderComposite.dispose();
        storageTypeComposite.dispose();

        shell.layout(true, true);
        shell.pack();

        shell.setMinimumSize(originalSize);
        shell.setSize(originalSize);

        Rectangle parentBounds = shell.getParent().getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation((parentBounds.x + (parentBounds.width / 2)) - (shellSize.x / 2),
                          (parentBounds.y + (parentBounds.height / 2)) - (shellSize.y / 2));
    }
}
