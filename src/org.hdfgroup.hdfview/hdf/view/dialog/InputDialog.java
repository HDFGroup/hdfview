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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import hdf.HDFVersions;
import hdf.view.ViewProperties;

/**
 * Custom SWT dialog to allow the user to input strings
 * for various uses.
 */
//TODO: Add ability to have custom HDF icons
public class InputDialog extends Dialog {
    private Text            inputField;
    private final String    title;
    private final String    message;
    private final String    initialText;
    private String          result;
    private Font            curFont;

    /**
     * Custom SWT dialog to allow the user to input strings
     * for a parent object.
     *
     * @param parent
     *        the dialog parent shell
     */
    public InputDialog(Shell parent) {
        this(parent, "HDFView " + HDFVersions.getPropertyVersionView(), "");
    }

    /**
     * Custom SWT dialog to allow the user to input strings
     * for a parent object with a title and message.
     *
     * @param parent
     *        the dialog parent shell
     * @param title
     *        the dialog title
     * @param message
     *        the dialog message
     */
    public InputDialog(Shell parent, String title, String message) {
        this(parent, title, message, "");
    }

    /**
     * Custom SWT dialog to allow the user to input strings
     * for a parent object with a title, message and style.
     *
     * @param parent
     *        the dialog parent shell
     * @param title
     *        the dialog title
     * @param message
     *        the dialog message
     * @param style
     *        the dialog style
     */
    public InputDialog(Shell parent, String title, String message, int style) {
        this(parent, title, message, "", style);
    }

    /**
     * Custom SWT dialog to allow the user to input strings
     * for a parent object with a title, message and initial text to be displayed.
     *
     * @param parent
     *        the dialog parent shell
     * @param title
     *        the dialog title
     * @param message
     *        the dialog message
     * @param initialText
     *        the dialog initialText
     */
    public InputDialog(Shell parent, String title, String message, String initialText) {
        this(parent, title, message, initialText, SWT.NONE);
    }

    /**
     * Custom SWT dialog to allow the user to input strings
     * for a parent object with a title, message, style and initial text to be displayed.
     *
     * @param parent
     *        the dialog parent shell
     * @param title
     *        the dialog title
     * @param message
     *        the dialog message
     * @param initialText
     *        the dialog initialText
     * @param style
     *        the dialog style
     */
    public InputDialog(Shell parent, String title, String message, String initialText, int style) {
        super(parent, style);
        this.title = title;
        this.message = message;
        this.initialText = initialText;

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
    }

    /**
     * Opens the InputDialog and returns the user's input
     * when the dialog closes.
     *
     * @return the user input data
     */
    public String open() {
        Shell parent = getParent();
        final Shell shell = new Shell(parent, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.RESIZE);
        shell.setFont(curFont);
        shell.setText(title);
        shell.setLayout(new GridLayout(1, true));

        Label label = new Label(shell, SWT.NULL);
        label.setFont(curFont);
        label.setText(message);

        inputField = new Text(shell, SWT.SINGLE | SWT.BORDER);
        inputField.setFont(curFont);
        inputField.setText(initialText);
        GridData fieldData = new GridData(SWT.FILL, SWT.FILL, true, false);
        fieldData.minimumWidth = 300;
        inputField.setLayoutData(fieldData);

        // Dummy label to fill space as dialog is resized
        new Label(shell, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite buttonComposite = new Composite(shell, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, true));
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        Button okButton = new Button(buttonComposite, SWT.PUSH);
        okButton.setFont(curFont);
        okButton.setText("   &OK   ");
        okButton.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
        okButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                shell.dispose();
            }
        });

        Button cancelButton = new Button(buttonComposite, SWT.PUSH);
        cancelButton.setFont(curFont);
        cancelButton.setText(" &Cancel ");
        cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
        cancelButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                result = null;
                shell.dispose();
            }
        });

        inputField.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event) {
                try {
                    result = inputField.getText();
                }
                catch (Exception ex) {
                    System.err.println("Error retrieving input value.");
                }
            }
        });

        shell.addListener(SWT.Traverse, new Listener() {
            public void handleEvent(Event event) {
                if(event.detail == SWT.TRAVERSE_ESCAPE)
                    event.doit = false;
            }
        });

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

        Display display = parent.getDisplay();
        while(!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }

        // TODO: Display loop should not wait here, but we must wait until
        // an input is given before returning
        return result;
    }
}
