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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import hdf.object.Datatype;
import hdf.object.HObject;

public class DefaultDatatypeMetaDataView extends DefaultLinkMetaDataView implements MetaDataView {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultDatatypeMetaDataView.class);

    public DefaultDatatypeMetaDataView(Composite parentComposite, ViewManager viewer, HObject theObj) {
        super(parentComposite, viewer, theObj);
    }

    @Override
    protected void addObjectSpecificContent() {
        log.trace("addObjectSpecificContent(): start");

        super.addObjectSpecificContent();

        org.eclipse.swt.widgets.Group datatypeInfoGroup = new org.eclipse.swt.widgets.Group(generalObjectInfoPane, SWT.NONE);
        datatypeInfoGroup.setFont(curFont);
        datatypeInfoGroup.setText("Type");
        datatypeInfoGroup.setLayout(new FillLayout());
        datatypeInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        Text infoArea = new Text(datatypeInfoGroup, SWT.MULTI);
        infoArea.setFont(curFont);
        infoArea.setText(((Datatype) dataObject).getDatatypeDescription());
        infoArea.setEditable(false);

        log.trace("addObjectSpecificContent(): finish");
    }

}
