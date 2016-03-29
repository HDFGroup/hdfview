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

import java.awt.Image;
import java.awt.Rectangle;

/**
 * The image view interface for displaying image object
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public abstract interface ImageView extends DataView {
    /**
     * Returns the selected area of the image
     *
     * @return the rectangle of the selected image area.
     */
    public abstract Rectangle getSelectedArea();

    /** @return true if the image is a truecolor image. */
    public abstract boolean isTrueColor();

    /** @return true if the image interlace is plane interlace. */
    public abstract boolean isPlaneInterlace();

    /** @return array of selected data */
    public abstract Object getSelectedData();

    /** @return the image displayed in this imageView */
    public abstract Image getImage();

    /** Sets the image
     *
     * @param img the image to view
     */
    public abstract void setImage(Image img);

    /** @return the palette of the image */
    public abstract byte[][] getPalette();

    /** Sets the image palette
     *
     * @param palette the palette for the image to view
     */
    public abstract void setPalette(byte[][] palette);

    /** @return the byte array of the image data */
    public abstract byte[] getImageByteData();

}
