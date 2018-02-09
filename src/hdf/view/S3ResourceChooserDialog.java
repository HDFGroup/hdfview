/* ***************************************************************************
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
 * **************************************************************************/

package hdf.view;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * GUI dialog to select files hosted on Amazon's S3 Object Store service
 *
 * User inputs url path and optionally access credentials:
 * AWS region of resource,
 * Access ID
 * Access "Secret" Key
 *
 * @author jake.smith
 */
public class S3ResourceChooserDialog extends Dialog {

    /* default enabling or disabling of Authentication input fields
     * upon dialog startup
     */
    private static boolean AUTH_ENABLED_DEFAULT = false;

    private boolean authenticate;
    private boolean exit_ok;
    private String  id;
    private String  key;
    private String  region;
    private String  url;

    private Button  authButton;
    private Font    curFont;
    private Shell   parent;
    private Text    idArea;
    private Text    keyArea;
    private Text    regionArea;
    private Text    urlArea;

    S3ResourceChooserDialog(Shell parent) {
        super(parent);

        this.parent       = parent;
        this.authenticate = false;
        this.exit_ok      = false;
        this.id           = "";
        this.key          = "";
        this.region       = "";
        this.url          = "";

        try {
            this.curFont = new Font(
                    Display.getCurrent(),
                    ViewProperties.getFontType(),
                    ViewProperties.getFontSize(),
                    SWT.NORMAL);
        }
        catch (Exception ex) {
            this.curFont = null;
        }
    }

    /**
     * callback to selection event, indicates "OK" to accept input
     */
    private void acceptUserInput() {
        this.exit_ok = true;
        this.url = this.urlArea.getText();
        if (this.authButton.getSelection()) {
            this.authenticate = true;
            this.region = this.regionArea.getText();
            this.id = this.idArea.getText();
            this.key = this.keyArea.getText();
        }
    }

    /**
     * Get this GUI going.
     */
    public void open() {
        Shell shell = new Shell(
                this.parent,
                SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        shell.setFont(this.curFont);
        shell.setText("Input S3 Information");
        shell.setLayout(new GridLayout(1, false));

        /* ************
         * URL REGION *
         * ************/

        Composite compositeURL = new Composite(shell, SWT.NONE);
        compositeURL.setLayout(new GridLayout(2, false));
        compositeURL.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, false, 2,1));
        Label urllabel = new Label(compositeURL, SWT.RIGHT);
        urllabel.setFont(this.curFont);
        urllabel.setText("URL to file on S3");
        this.urlArea = new Text(compositeURL, SWT.NONE);
        this.urlArea.setFont(this.curFont);
        this.urlArea.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, false));

        /* ***********************
         * AUTHENTICATION REGION *
         * ***********************/

        Group groupCred = new Group(shell, SWT.NONE);
        groupCred.setLayout(new GridLayout(1, false));
        groupCred.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        groupCred.setFont(this.curFont);
        groupCred.setText("Authentication");

        this.authButton = new Button(groupCred, SWT.CHECK);
        this.authButton.setFont(this.curFont);
        this.authButton.setText("Authenticate with credentials");
        this.authButton.setSelection(AUTH_ENABLED_DEFAULT);
        this.authButton.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, false));
        this.authButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                idArea.setEnabled(authButton.getSelection());
                regionArea.setEnabled(authButton.getSelection());
                keyArea.setEnabled(authButton.getSelection());
            }
        });

        Composite compositeAuth = new Composite(groupCred, SWT.NONE);
        compositeAuth.setLayout(new GridLayout(2, false));
        compositeAuth.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        Label regionLabel = new Label(compositeAuth, SWT.RIGHT);
        regionLabel.setFont(this.curFont);
        regionLabel.setText("AWS Region");
        this.regionArea = new Text(compositeAuth, SWT.NONE);
        this.regionArea.setFont(this.curFont);
        this.regionArea.setEnabled(AUTH_ENABLED_DEFAULT);
        this.regionArea.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, false));

        Label idLabel = new Label(compositeAuth, SWT.RIGHT);
        idLabel.setFont(this.curFont);
        idLabel.setText("Access ID");
        this.idArea = new Text(compositeAuth, SWT.NONE);
        this.idArea.setFont(this.curFont);
        this.idArea.setEnabled(AUTH_ENABLED_DEFAULT);
        this.idArea.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, false));

        Label keyLabel = new Label(compositeAuth, SWT.RIGHT);
        keyLabel.setFont(this.curFont);
        keyLabel.setText("Access Key");
        this.keyArea = new Text(compositeAuth, SWT.NONE);
        this.keyArea.setFont(this.curFont);
        this.keyArea.setEnabled(AUTH_ENABLED_DEFAULT);
        this.keyArea.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, false));

        /* *************
         * OK / CANCEL *
         * *************/

        Composite compositeOkExit = new Composite(shell, SWT.NONE);
        compositeOkExit.setLayout(new GridLayout(2, false));
        compositeOkExit.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        Button okButton = new Button(compositeOkExit, SWT.PUSH);
        okButton.setFont(this.curFont);
        okButton.setText("   &Ok   ");
        okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
        okButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                acceptUserInput();
                shell.dispose();
            }
        });

        Button cancelButton = new Button(compositeOkExit, SWT.PUSH);
        cancelButton.setFont(this.curFont);
        cancelButton.setText("&Cancel");
        cancelButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
        cancelButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                shell.dispose();
            }
        });

        /* ********************
         * PREPARE TO DISPLAY *
         * ********************/

        shell.pack();

        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (curFont != null) curFont.dispose();
            }
        });

        Rectangle parentBounds = this.parent.getBounds();
        Point shellSize = shell.getSize();
        shell.setLocation(
                (parentBounds.x + parentBounds.width / 2) - (shellSize.x / 2),
                (parentBounds.y + parentBounds.height / 2) - (shellSize.y / 2));

        shell.open();

        Display display = this.parent.getDisplay();
        while(!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    /**
     * Get the credentials as supplied by the user.
     *
     * Maps each element to lowercase string key: url, region, id, key.
     * Empty strings indicate that user did not supply anything.
     *
     * Should be called only if hasInput() is true.
     *
     * @return Mapping of url, region, id, and key to user-provided values.
     *         key 'authenticate' is empty if anonymous, "true" otherwise
     */
    public Map<String, String> getInput() throws Exception {
        if (!this.exit_ok)
            throw new Exception(
                    "Dialog either did not exit or was closed in an "+
                    "unrecognized manner. Unable to get input\n" +
                    this.toString());
        if (this.url.equals(""))
            throw new Exception(
                    "File URL was not specified by user--invalid\n" +
                    this.toString());
        if (this.authenticate && (this.id.equals("") || this.region.equals("")))
            throw new Exception("Authentication was specified but ID and/or "+
                    "Region was not provided.\n" +
                    this.toString());
        Map<String, String> ret = new HashMap<>(4);
        ret.put("url",    this.url);
        ret.put("authenticate", (this.authenticate) ? "true" : "");
        ret.put("region", this.region);
        ret.put("id",     this.id);
        ret.put("key",    this.key);
        return ret;
    }

    @Override
    public String toString() {
        String ok   = (this.exit_ok)      ? "true" : "false" ;
        String auth = (this.authenticate) ? "true" : "false";
        return "S3ResourceChooserDialog:" +
                "\n  exit_ok      : " + ok +
                "\n  url          : " + this.url +
                "\n  authenticate : " + auth +
                "\n  region       : " + this.region +
                "\n  id           : " + this.id +
                "\n  key          : " + this.key + "\n";
    }
}

