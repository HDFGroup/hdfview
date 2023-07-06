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
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import hdf.view.ViewProperties;


/**
 * UserOptionsViewModulesPage.java - Configuration page for user-implementable
 * modules.
 */
public class UserOptionsViewModulesPage extends UserOptionsDefaultPage {
    private static final Logger log = LoggerFactory.getLogger(UserOptionsViewModulesPage.class);

    private Combo                 choiceTreeView, choiceMetaDataView, choiceTableView, choiceImageView, choicePaletteView;

    /** A list of Tree view implementations. */
    private static ArrayList<String> treeViews;

    /** A list of Image view implementations. */
    private static ArrayList<String> imageViews;

    /** A list of Table view implementations. */
    private static ArrayList<String> tableViews;

    /** A list of metadata view implementations. */
    private static ArrayList<String> metaDataViews;

    /** A list of palette view implementations. */
    private static ArrayList<String> paletteViews;

    /**
     * Configuration page for user-implementable modules.
     */
    public UserOptionsViewModulesPage() {
        super("View Modules Settings");
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
     * @return <code>false</code> to abort the container's OK processing and <code>true</code> to allow
     *         the OK to happen
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean performOk() {
        getPreferenceStore();

        ArrayList[] moduleList = { treeViews, metaDataViews, tableViews, imageViews, paletteViews };
        Combo[] choiceList = { choiceTreeView, choiceMetaDataView, choiceTableView, choiceImageView, choicePaletteView };
        for (int i = 0; i < moduleList.length; i++) {
            Combo curModuleCombo = choiceList[i];
            if (curModuleCombo != null) {
                Object theModule = curModuleCombo.getItem(curModuleCombo.getSelectionIndex());
                moduleList[i].remove(theModule);
                moduleList[i].add(0, theModule);
            }
        }

        return true;
    }

    /**
     * Loads all stored values in the <code>FieldEditor</code>s.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void load() {
        getPreferenceStore();

        treeViews = (ArrayList<String>) ViewProperties.getTreeViewList();
        metaDataViews = (ArrayList<String>) ViewProperties.getMetaDataViewList();
        tableViews = (ArrayList<String>) ViewProperties.getTableViewList();
        imageViews = (ArrayList<String>) ViewProperties.getImageViewList();
        paletteViews = (ArrayList<String>) ViewProperties.getPaletteViewList();
        // srbVector = (ArrayList<String>)ViewProperties.getSrbAccount();

        choiceTreeView.setItems(treeViews.toArray(new String[0]));
        choiceTreeView.select(0);
        choiceMetaDataView.setItems(metaDataViews.toArray(new String[0]));
        choiceMetaDataView.select(0);
        choiceTableView.setItems(tableViews.toArray(new String[0]));
        choiceTableView.select(0);
        choiceImageView.setItems(imageViews.toArray(new String[0]));
        choiceImageView.select(0);
        choicePaletteView.setItems(paletteViews.toArray(new String[0]));
        choicePaletteView.select(0);

        ArrayList[] moduleList = { treeViews, metaDataViews, tableViews, imageViews, paletteViews };
        Combo[] choiceList = { choiceTreeView, choiceMetaDataView, choiceTableView, choiceImageView, choicePaletteView };
        for (int i = 0; i < moduleList.length; i++) {
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
    @Override
    protected Control createContents(Composite parent) {
        shell = parent.getShell();
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());

        org.eclipse.swt.widgets.Group treeViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        treeViewGroup.setLayout(new FillLayout());
        treeViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        treeViewGroup.setFont(curFont);
        treeViewGroup.setText("TreeView Provider");

        choiceTreeView = new Combo(treeViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choiceTreeView.setFont(curFont);

        org.eclipse.swt.widgets.Group metadataViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        metadataViewGroup.setLayout(new FillLayout());
        metadataViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        metadataViewGroup.setFont(curFont);
        metadataViewGroup.setText("MetaDataView Provider");

        choiceMetaDataView = new Combo(metadataViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choiceMetaDataView.setFont(curFont);

        org.eclipse.swt.widgets.Group tableViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        tableViewGroup.setLayout(new FillLayout());
        tableViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        tableViewGroup.setFont(curFont);
        tableViewGroup.setText("TableView Provider");

        choiceTableView = new Combo(tableViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choiceTableView.setFont(curFont);

        org.eclipse.swt.widgets.Group imageViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        imageViewGroup.setLayout(new FillLayout());
        imageViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        imageViewGroup.setFont(curFont);
        imageViewGroup.setText("ImageView Provider");

        choiceImageView = new Combo(imageViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choiceImageView.setFont(curFont);

        org.eclipse.swt.widgets.Group paletteViewGroup = new org.eclipse.swt.widgets.Group(composite, SWT.NONE);
        paletteViewGroup.setLayout(new FillLayout());
        paletteViewGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        paletteViewGroup.setFont(curFont);
        paletteViewGroup.setText("PaletteView Provider");

        choicePaletteView = new Combo(paletteViewGroup, SWT.SINGLE | SWT.READ_ONLY);
        choicePaletteView.setFont(curFont);

        load();
        return composite;

    }
}
