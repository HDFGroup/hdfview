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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import hdf.object.Datatype;
import hdf.object.HObject;
import hdf.view.DataView.DataViewManager;

/**
 *
 *The metadata view interface for displaying datatype metadata information
 */
public class DefaultDatatypeMetaDataView extends DefaultLinkMetaDataView implements MetaDataView {

    private static final Logger log = LoggerFactory.getLogger(DefaultDatatypeMetaDataView.class);

    /**
     *The metadata view interface for displaying datatype metadata information
     *
     * @param parentComposite
     *        the parent visual object
     * @param viewer
     *        the viewer to use
     * @param theObj
     *        the object to display the metadata info
     */
    public DefaultDatatypeMetaDataView(Composite parentComposite, DataViewManager viewer, HObject theObj) {
        super(parentComposite, viewer, theObj);
    }

    @Override
    protected void addObjectSpecificContent() {
        super.addObjectSpecificContent();

        org.eclipse.swt.widgets.Group datatypeInfoGroup = new org.eclipse.swt.widgets.Group(generalObjectInfoPane, SWT.NONE);
        datatypeInfoGroup.setFont(curFont);
        datatypeInfoGroup.setText("Type");
        datatypeInfoGroup.setLayout(new FillLayout());
        datatypeInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        Text infoArea = new Text(datatypeInfoGroup, SWT.MULTI);
        infoArea.setFont(curFont);
        infoArea.setText(((Datatype) dataObject).getDescription());
        infoArea.setEditable(false);
    }

}
