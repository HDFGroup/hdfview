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

package hdf.view.MetaDataView;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.object.h5.H5Link;
import hdf.view.Tools;
import hdf.view.DataView.DataViewManager;

/**
 *
 * The metadata view interface for displaying link metadata information
 */
public class DefaultLinkMetaDataView extends DefaultBaseMetaDataView implements MetaDataView {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultLinkMetaDataView.class);

    /**
     *The metadata view interface for displaying link metadata information
     *
     * @param parentComposite
     *        the parent visual object
     * @param viewer
     *        the viewer to use
     * @param theObj
     *        the object to display the metadata info
     */
    public DefaultLinkMetaDataView(Composite parentComposite, DataViewManager viewer, HObject theObj) {
        super(parentComposite, viewer, theObj);
    }

    @Override
    protected void addObjectSpecificContent() {
        /* For HDF5 links, add a box to allow changing of the link target */
        if (dataObject.getLinkTargetObjName() != null) {
            org.eclipse.swt.widgets.Group linkTargetGroup = new org.eclipse.swt.widgets.Group(generalObjectInfoPane, SWT.NONE);
            linkTargetGroup.setFont(curFont);
            linkTargetGroup.setText("Link Target Info");
            linkTargetGroup.setLayout(new GridLayout(2, false));

            /*
             * If this object is purely a link, meaning a soft/external/other link that does
             * not point to an existing object, allow the Link Target Info Group to take up
             * all the available vertical space in the MetaDataView.
             *
             * Otherwise, if this link points to an existing object, it will not be an
             * instanceof H5Link. Rather, it will be an instanceof Dataset, instanceof
             * Group, etc. and will contain the information necessary to determine that the
             * object is really a link.
             *
             * In this case, we don't allow the Link Target Info Group to take up all the
             * available vertical space, in order to allow other object Metadata information
             * to be comfortable displayed in the MetaDataView.
             */
            if (dataObject instanceof H5Link)
                linkTargetGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
            else
                linkTargetGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

            Label label = new Label(linkTargetGroup, SWT.LEFT);
            label.setFont(curFont);
            label.setText("Link To Target: ");

            final Text linkTarget = new Text(linkTargetGroup, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
            linkTarget.setFont(curFont);
            linkTarget.setText(dataObject.getLinkTargetObjName());
            linkTarget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            linkTarget.addTraverseListener(new TraverseListener() {
                @Override
                public void keyTraversed(TraverseEvent e) {
                    if (e.detail == SWT.TRAVERSE_RETURN) {
                        changeLinkTarget(linkTarget.getText());
                    }
                }
            });
            linkTarget.addListener(SWT.FocusOut, new Listener() {
                @Override
                public void handleEvent(Event arg0) {
                    changeLinkTarget(linkTarget.getText());
                }
            });
        }
    }

    private void changeLinkTarget(String linkTargetName) {
        Group pgroup = null;
        try {
            pgroup = (Group) dataObject.getFileFormat().get(dataObject.getPath());
        }
        catch (Exception ex) {
            log.debug("addObjectSpecificContent(): parent group get failure:", ex);
        }

        if (pgroup == null) {
            log.debug("addObjectSpecificContent(): parent group is null");
            display.beep();
            Tools.showError(display.getShells()[0], "Select", "Parent group is null.\nLink target change failed.");
            return;
        }

        if (linkTargetName != null)
            linkTargetName = linkTargetName.trim();

        int linkType = Group.LINK_TYPE_SOFT;
        if (dataObject.getLinkTargetObjName().contains(FileFormat.FILE_OBJ_SEP))
            linkType = Group.LINK_TYPE_EXTERNAL;
        else if ((linkTargetName != null) && (linkTargetName.equals("/"))) { // do not allow to link to the root
            display.beep();
            Tools.showError(display.getShells()[0], "Select", "Link to root not allowed.\nLink target change failed.");
            return;
        }

        // no change
        if ((linkTargetName != null) && (linkTargetName.equals(dataObject.getLinkTargetObjName())))
            return;

        // invalid name
        if (linkTargetName == null || linkTargetName.length() < 1) return;

        try {
            dataObject.getFileFormat().createLink(pgroup, dataObject.getName(), linkTargetName, linkType);
            dataObject.setLinkTargetObjName(linkTargetName);
        }
        catch (Exception ex) {
            log.debug("addObjectSpecificContent(): createLink() failure:", ex);
            display.beep();
            Tools.showError(display.getShells()[0], "Select", "Link target change failed." + ex.getMessage());
            return;
        }

        Tools.showInformation(display.getShells()[0], "Link target changed.",
                "Reload file to display changes.");
    }

}
