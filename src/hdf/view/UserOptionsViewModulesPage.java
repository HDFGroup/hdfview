/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see https://support.hdfgroup.org/products/licenses.html               *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.view;

import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


/**
 * UserOptionsGeneralPage.java - Configuration page for general application settings.
 */
public class UserOptionsViewModulesPage extends UserOptionsDefaultPage {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserOptionsViewModulesPage.class);

    private Combo                 choiceTreeView, choiceMetaDataView, choiceTextView, choiceTableView, choiceImageView, choicePaletteView;

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

    public UserOptionsViewModulesPage() {
        super("View Modules Settings");
    }
    /**
     * Notifies that the OK button of this page's container has been pressed.
     *
     * @return <code>false</code> to abort the container's OK processing and
     * <code>true</code> to allow the OK to happen
     */
    public boolean performOk() {
        ViewProperties store = (ViewProperties)getPreferenceStore();

        return true;
    }

    /**
     * Loads all stored values in the <code>FieldEditor</code>s.
     */
    protected void load() {
        ViewProperties store = (ViewProperties)getPreferenceStore();

        treeViews = ViewProperties.getTreeViewList();
        metaDataViews = ViewProperties.getMetaDataViewList();
        textViews = ViewProperties.getTextViewList();
        tableViews = ViewProperties.getTableViewList();
        imageViews = ViewProperties.getImageViewList();
        paletteViews = ViewProperties.getPaletteViewList();
        // srbVector = ViewProperties.getSrbAccount();

        choiceTreeView.setItems(treeViews.toArray(new String[0]));
        choiceTreeView.select(0);
        choiceMetaDataView.setItems(metaDataViews.toArray(new String[0]));
        choiceMetaDataView.select(0);
        choiceTextView.setItems(textViews.toArray(new String[0]));
        choiceTextView.select(0);
        choiceTableView.setItems(tableViews.toArray(new String[0]));
        choiceTableView.select(0);
        choiceImageView.setItems(imageViews.toArray(new String[0]));
        choiceImageView.select(0);
        choicePaletteView.setItems(paletteViews.toArray(new String[0]));
        choicePaletteView.select(0);

        @SuppressWarnings("rawtypes")
        Vector[] moduleList = { treeViews, metaDataViews, textViews, tableViews, imageViews, paletteViews };
        Combo[] choiceList = { choiceTreeView, choiceMetaDataView, choiceTextView, choiceTableView,
                choiceImageView, choicePaletteView };
        for (int i = 0; i < 6; i++) {
            Object theModule = choiceList[i].getItem(choiceList[i].getSelectionIndex());
            moduleList[i].remove(theModule);
            moduleList[i].add(0, theModule);
        }
    }

    /**
     * Creates and returns the SWT control for the customized body of this
     * preference page under the given parent composite.
     *
     * @param parent the parent composite
     * @return the new control
     */
    protected Control createContents(Composite parent) {
        shell = parent.getShell();
        ScrolledComposite scroller = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        scroller.setExpandHorizontal(true);
        scroller.setExpandVertical(true);
        scroller.setMinSize(shell.getSize());

        Composite composite = new Composite(scroller, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        scroller.setContent(composite);

        org.eclipse.swt.widgets.Group treeViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        treeViewGroup.setLayout(new FillLayout());
        treeViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        treeViewGroup.setFont(curFont);
        treeViewGroup.setText("TreeView");

        choiceTreeView = new Combo(treeViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choiceTreeView.setFont(curFont);

        org.eclipse.swt.widgets.Group metadataViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        metadataViewGroup.setLayout(new FillLayout());
        metadataViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        metadataViewGroup.setFont(curFont);
        metadataViewGroup.setText("MetaDataView");

        choiceMetaDataView = new Combo(metadataViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choiceMetaDataView.setFont(curFont);

        org.eclipse.swt.widgets.Group textViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        textViewGroup.setLayout(new FillLayout());
        textViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        textViewGroup.setFont(curFont);
        textViewGroup.setText("TextView");

        choiceTextView = new Combo(textViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choiceTextView.setFont(curFont);

        org.eclipse.swt.widgets.Group tableViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        tableViewGroup.setLayout(new FillLayout());
        tableViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        tableViewGroup.setFont(curFont);
        tableViewGroup.setText("TableView");

        choiceTableView = new Combo(tableViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choiceTableView.setFont(curFont);

        org.eclipse.swt.widgets.Group imageViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        imageViewGroup.setLayout(new FillLayout());
        imageViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        imageViewGroup.setFont(curFont);
        imageViewGroup.setText("ImageView");

        choiceImageView = new Combo(imageViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choiceImageView.setFont(curFont);

        org.eclipse.swt.widgets.Group paletteViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        paletteViewGroup.setLayout(new FillLayout());
        paletteViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        paletteViewGroup.setFont(curFont);
        paletteViewGroup.setText("PaletteView");

        choicePaletteView = new Combo(paletteViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choicePaletteView.setFont(curFont);

        load();
        return scroller;
    }
}
