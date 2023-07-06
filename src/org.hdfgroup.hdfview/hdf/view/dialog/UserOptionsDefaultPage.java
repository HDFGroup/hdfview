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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * <code>UserOptionsDefaultPage</code> extends <code>PreferencePage</code>
 * to display the default button
 * images for the ok, cancel, apply and defaults button. All configuration pages
 * that need finer control on the created editor <code>Controls</code> should
 * inherit this class.
 */
public abstract class UserOptionsDefaultPage extends PreferencePage {

    private static final Logger log = LoggerFactory.getLogger(UserOptionsDefaultPage.class);

    /** The reference to the visual shell */
    protected Shell                 shell;

    /** The setting of the current font */
    protected Font                  curFont;

    /** The setting of the root directory */
    protected String                rootDir = null;

    /**
     * <code>UserOptionsDefaultPage</code> default constructor.
     */
    public UserOptionsDefaultPage() {
        super();
    }

    /**
     * Creates a new abstract <code>UserOptionsDefaultPage</code> with the
     * given title.
     *
     * @param title the page title
     */
    public UserOptionsDefaultPage(String title) {
        super(title);
    }

    /**
     * Creates a new abstract <code>UserOptionsDefaultPage</code> with the
     * given title and image.
     *
     * @param title the page title
     * @param image the image for this page, or <code>null</code> if none
     */
    public UserOptionsDefaultPage(String title, ImageDescriptor image) {
        super(title, image);
    }

    @Override
    public void createControl(Composite parent)
    {
        super.createControl(parent);

        Button applyButton = this.getApplyButton();
        Button defaultsButton = this.getDefaultsButton();
        if (applyButton != null && defaultsButton != null) {
            /* Apply and default button are shown */

            /* Customize apply button (text + image) */
            applyButton.setText("apply changes");
            //applyButton.setImage(SWTHelper.loadImage("save.gif"));
            this.setButtonLayoutData(applyButton);

            /* Customize defaults button (text + image) */
            defaultsButton.setText("restore defaults");
            //defaultsButton.setImage(SWTHelper.loadImage("clear.gif"));
            this.setButtonLayoutData(defaultsButton);
        }
    }
}
