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

package hdf.view.dialog;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Shell;

/**
 * UserOptionsDialog displays components for choosing user options.
 *
 * @author Jordan T. Henderson
 * @version 2.4 2/13/2016
 */
public class UserOptionsDialog extends PreferenceDialog {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserOptionsDialog.class);

    private Shell                         shell;

    private Font                          curFont;

    protected String                      rootDir = null;

    protected String                      workDir = null;

    public UserOptionsDialog(Shell parent, PreferenceManager mgr, String viewRoot) {
        super(parent, mgr);

        rootDir = viewRoot;
    }

    public void create() {
        super.create();
        getShell().setSize(getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
    }
}
