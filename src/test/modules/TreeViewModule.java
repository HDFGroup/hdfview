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

package test.modules;

import java.util.List;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;
import hdf.view.DataView.DataView;
import hdf.view.MetaDataView.MetaDataView;
import hdf.view.TreeView.TreeView;

public class TreeViewModule implements TreeView {

    public TreeViewModule() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public FileFormat openFile(String filename, int accessID) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileFormat reopenFile(FileFormat theFile, int newFileAccessMode) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void closeFile(FileFormat file) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void saveFile(FileFormat file) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDefaultDisplayMode(boolean displaymode) {
        // TODO Auto-generated method stub

    }

    @Override
    public FileFormat getSelectedFile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HObject getCurrentObject() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataView showDataContent(HObject dataObject) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MetaDataView showMetaData(HObject dataObject) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeItem addObject(HObject newObject, Group parentGroup) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tree getTree() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<FileFormat> getCurrentFiles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeItem findTreeItem(HObject obj) {
        // TODO Auto-generated method stub
        return null;
    }

}
