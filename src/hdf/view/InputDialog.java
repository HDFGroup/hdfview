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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import hdf.HDFVersions;

/**
 * Custom SWT dialog to allow the user to input strings
 * for various uses.
 */
//TODO: Add ability to have custom HDF icons
public class InputDialog extends Dialog {
    private Text inputField;
    private final String title;
    private final String message;
    private final String initialText;
    private String result;
    
    public InputDialog(Shell parent) {
        super(parent, SWT.NONE);
        this.title = "HDFView " + HDFVersions.HDFVIEW_VERSION;
        this.message = null;
        this.initialText = "";
    }
    
    public InputDialog(Shell parent, String title, String message) {
        super(parent, SWT.NONE);
        this.title = title;
        this.message = message;
        this.initialText = "";
    }
    
    public InputDialog(Shell parent, String title, String message, String initialText) {
        super(parent, SWT.NONE);
        this.title = title;
        this.message = message;
        this.initialText = initialText;
    }
    
    public InputDialog(Shell parent, int style, String title, String message) {
        super(parent, style);
        this.title = title;
        this.message = message;
        this.initialText = "";
    }
    
    public InputDialog(Shell parent, int style, String title, String message, String initialText) {
        super(parent, style);
        this.title = title;
        this.message = message;
        this.initialText = initialText;
    }
    
    /**
     * Opens the InputDialog and returns the user's input
     * when the dialog closes.
     * @return
     */
    public String open() {
        Shell parent = getParent();
        final Shell shell = new Shell(parent, SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
        
        shell.setText(title);
        shell.setLayout(new GridLayout(2, true));
        
        Label label = new Label(shell, SWT.NULL);
        label.setText(message);
        
        inputField = new Text(shell, SWT.SINGLE | SWT.BORDER);
        inputField.setText(initialText);
        
        final Button okButton = new Button(shell, SWT.PUSH);
        okButton.setText("Ok");
        okButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        
        final Button cancelButton = new Button(shell, SWT.PUSH);
        cancelButton.setText("Cancel");
        
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
        
        okButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                shell.dispose();
            }
        });
        
        cancelButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                result = null;
                shell.dispose();
            }
        });
        
        shell.addListener(SWT.Traverse, new Listener() {
            public void handleEvent(Event event) {
                if(event.detail == SWT.TRAVERSE_ESCAPE)
                    event.doit = false;
            }
        });
        
        shell.pack();
        
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
        
        return result;
    }
}
