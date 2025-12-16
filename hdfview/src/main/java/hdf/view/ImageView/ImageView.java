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

package hdf.view.ImageView;

import java.awt.Image;
import java.awt.Rectangle;

import hdf.view.DataView.DataView;

/**
 * The image view interface for displaying image object.
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public interface ImageView extends DataView {
    /**
     * Returns the selected area of the image.
     *
     * @return the rectangle of the selected image area.
     */
    Rectangle getSelectedArea();

    /**
     * Check if the image is a truecolor image.
     *
     * @return true if the image is a truecolor image.
     */
    boolean isTrueColor();

    /**
     * Check if the image interlace is plane interlace.
     *
     * @return true if the image interlace is plane interlace.
     */
    boolean isPlaneInterlace();

    /**
     * Get the array of selected data.
     *
     * @return array of selected data
     */
    Object getSelectedData();

    /**
     * Get the image displayed in this imageView.
     *
     * @return the image displayed in this imageView
     */
    Image getImage();

    /**
     * Sets the image.
     *
     * @param img the image to view
     */
    void setImage(Image img);

    /**
     * Get the palette of the image.
     *
     * @return the palette of the image
     */
    byte[][] getPalette();

    /**
     * Sets the image palette.
     *
     * @param palette the palette for the image to view
     */
    void setPalette(byte[][] palette);

    /**
     * Get the byte array of the image data.
     *
     * @return the byte array of the image data
     */
    byte[] getImageByteData();
}
