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

import java.awt.Image;
import java.awt.Rectangle;

import hdf.object.HObject;
import hdf.view.ImageView.ImageView;

public class ImageViewModule implements ImageView {

    public ImageViewModule() {

    }

    @Override
    public HObject getDataObject() {
        return null;
    }

    @Override
    public Rectangle getSelectedArea() {
        return null;
    }

    @Override
    public boolean isTrueColor() {
        return false;
    }

    @Override
    public boolean isPlaneInterlace() {
        return false;
    }

    @Override
    public Object getSelectedData() {
        return null;
    }

    @Override
    public Image getImage() {
        return null;
    }

    @Override
    public void setImage(Image img) {

    }

    @Override
    public byte[][] getPalette() {
        return null;
    }

    @Override
    public void setPalette(byte[][] palette) {

    }

    @Override
    public byte[] getImageByteData() {
        return null;
    }

}
