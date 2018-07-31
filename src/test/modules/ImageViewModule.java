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

import java.awt.Image;
import java.awt.Rectangle;

import hdf.object.HObject;
import hdf.view.ImageView.ImageView;

public class ImageViewModule implements ImageView {

    public ImageViewModule() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public HObject getDataObject() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Rectangle getSelectedArea() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isTrueColor() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPlaneInterlace() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Object getSelectedData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Image getImage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setImage(Image img) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[][] getPalette() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setPalette(byte[][] palette) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] getImageByteData() {
        // TODO Auto-generated method stub
        return null;
    }

}
