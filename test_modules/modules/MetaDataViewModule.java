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

package modules;

import hdf.object.Attribute;
import hdf.object.HObject;
import hdf.view.MetaDataView.MetaDataView;

public class MetaDataViewModule implements MetaDataView {

    public MetaDataViewModule() {

    }

    @Override
    public HObject getDataObject() {
        return null;
    }

    @Override
    public Attribute addAttribute(HObject obj) {
        return null;
    }

    @Override
    public Attribute deleteAttribute(HObject obj) {
        return null;
    }

}
