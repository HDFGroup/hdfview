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

package hdf.object;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A scalar dataset is a multiple dimension array of scalar points. The Datatype of a scalar dataset must be an atomic
 * datatype. Common datatypes of scalar datasets include char, byte, short, int, long, float, double and string.
 *
 * A ScalarDS can be an image or spreadsheet data. ScalarDS defines methods to deal with both images and
 * spreadsheets.
 *
 * ScalarDS is an abstract class. Current implementing classes are the H4SDS, H5GRImage and H5ScalarDS.
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public abstract class ScalarDS extends Dataset
{
    private static final long serialVersionUID = 8925371455928203981L;

    private static final Logger log = LoggerFactory.getLogger(ScalarDS.class);

    /************************************************************
     * The following constant strings are copied from * https://hdfgroup.github.io/hdf5/_i_m_g.html * to make the
     * definition consistent with the image specs. *
     ************************************************************/

    /**
     * Indicates that the pixel RGB values are contiguous.
     */
    public static final int INTERLACE_PIXEL = 0;

    /** Indicates that each pixel component of RGB is stored as a scan line. */
    public static final int INTERLACE_LINE = 1;

    /** Indicates that each pixel component of RGB is stored as a plane. */
    public static final int INTERLACE_PLANE = 2;

    /**
     * The interlace mode of the stored raster image data. Valid values are INTERLACE_PIXEL, INTERLACE_LINE and
     * INTERLACE_PLANE.
     */
    protected int interlace;

    /**
     * The min-max range of image data values. For example, [0, 255] indicates the min is 0, and the max is 255.
     */
    protected double[] imageDataRange;

    /**
     * The indexed RGB color model with 256 colors.
     *
     * The palette values are stored in a two-dimensional byte array and arrange by color components of red, green and
     * blue. palette[][] = byte[3][256], where, palette[0][], palette[1][] and palette[2][] are the red, green and blue
     * components respectively.
     */
    protected byte[][] palette;

    /**
     * True if this dataset is a true color image.
     */
    protected boolean isTrueColor;

    /**
     * Flag to indicate is the original unsigned C data is converted.
     */
    protected boolean unsignedConverted;

    /** The fill value of the dataset. */
    protected Object fillValue = null;

    /** The list of filtered image values. */
    private List<Number> filteredImageValues;

    /** Flag to indicate if the dataset is displayed as an image. */
    protected boolean isImageDisplay;

    /**
     * Flag to indicate if the dataset is displayed as an image with default order of dimensions.
     */
    protected boolean isDefaultImageOrder;

    /**
     * Flag to indicate if the FillValue is converted from unsigned C.
     */
    public boolean isFillValueConverted;

    /**
     * Constructs an instance of a ScalarDS with specific name and path. An HDF data object must have a name. The path
     * is the group path starting from the root.
     *
     * For example, in H5ScalarDS(h5file, "dset", "/arrays/"), "dset" is the name of the dataset, "/arrays" is the group
     * path of the dataset.
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the full path of the data object, e.g. "/arrays/".
     */
    public ScalarDS(FileFormat theFile, String theName, String thePath) {
        this(theFile, theName, thePath, null);
    }

    /**
     * @deprecated Not for public use in the future.<br>
     *             Using {@link #ScalarDS(FileFormat, String, String)}
     *
     * @param theFile
     *            the file that contains the data object.
     * @param theName
     *            the name of the data object, e.g. "dset".
     * @param thePath
     *            the full path of the data object, e.g. "/arrays/".
     * @param oid
     *            the object id of the data object.
     */
    @Deprecated
    public ScalarDS(FileFormat theFile, String theName, String thePath, long[] oid) {
        super(theFile, theName, thePath, oid);

        palette = null;
        isImage = false;
        isTrueColor = false;
        isText = false;
        interlace = -1;
        imageDataRange = null;
        isImageDisplay = false;
        isDefaultImageOrder = true;
        isFillValueConverted = false;
        filteredImageValues = new Vector<>();
    }

    /**
     * Clears the current data buffer in memory and forces the next read() to load
     * the data from file.
     *
     * The function read() loads data from file into memory only if the data is not
     * read. If data is already in memory, read() just returns the memory buffer.
     * Sometimes we want to force read() to re-read data from file. For example,
     * when the selection is changed, we need to re-read the data.
     *
     * @see #getData()
     * @see #read()
     */
    @Override
    public void clearData() {
        super.clearData();
        unsignedConverted = false;
    }

    /**
     * Converts the data values of this dataset to appropriate Java integer if they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertFromUnsignedC(Object, Object)
     *
     * @return the converted data buffer.
     */
    @Override
    public Object convertFromUnsignedC() {
        // keep a copy of original buffer and the converted buffer
        // so that they can be reused later to save memory
        log.trace("convertFromUnsignedC(): unsigned={}", getDatatype().isUnsigned());
        if ((data != null) && getDatatype().isUnsigned() && !unsignedConverted) {
            log.trace("convertFromUnsignedC(): convert");
            originalBuf = data;
            convertedBuf = convertFromUnsignedC(originalBuf, convertedBuf);
            data = convertedBuf;
            unsignedConverted = true;

            if (fillValue != null) {
                if (!isFillValueConverted) {
                    fillValue = convertFromUnsignedC(fillValue, null);
                    isFillValueConverted = true;
                }
            }
        }

        return data;
    }

    /**
     * Converts Java integer data of this dataset back to unsigned C-type integer data if they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertToUnsignedC(Object, Object)
     * @see #convertFromUnsignedC(Object data_in)
     *
     * @return the converted data buffer.
     */
    @Override
    public Object convertToUnsignedC() {
        // keep a copy of original buffer and the converted buffer
        // so that they can be reused later to save memory
        log.trace("convertToUnsignedC(): unsigned={}", getDatatype().isUnsigned());
        if ((data != null) && getDatatype().isUnsigned()) {
            log.trace("convertToUnsignedC(): convert");
            convertedBuf = data;
            originalBuf = convertToUnsignedC(convertedBuf, originalBuf);
            data = originalBuf;
        }

        return data;
    }

    /**
     * Returns the palette of this scalar dataset or null if palette does not exist.
     *
     * A Scalar dataset can be displayed as spreadsheet data or an image. When a scalar dataset is displayed as an
     * image, the palette or color table may be needed to translate a pixel value to color components (for example, red,
     * green, and blue). Some scalar datasets have no palette and some datasets have one or more than one palettes. If
     * an associated palette exists but is not loaded, this interface retrieves the palette from the file and returns the
     * palette. If the palette is loaded, it returns the palette. It returns null if there is no palette associated with
     * the dataset.
     *
     * Current implementation only supports palette model of indexed RGB with 256 colors. Other models such as
     * YUV", "CMY", "CMYK", "YCbCr", "HSV will be supported in the future.
     *
     * The palette values are stored in a two-dimensional byte array and are arranges by color components of red, green and
     * blue. palette[][] = byte[3][256], where, palette[0][], palette[1][] and palette[2][] are the red, green and blue
     * components respectively.
     *
     * Sub-classes have to implement this interface. HDF4 and HDF5 images use different libraries to retrieve the
     * associated palette.
     *
     * @return the 2D palette byte array.
     */
    public byte[][] getPalette() {
        return palette;
    }

    /**
     * Sets the palette for this dataset.
     *
     * @param pal
     *            the 2D palette byte array.
     */
    public final void setPalette(byte[][] pal) {
        palette = pal;
    }

    /**
     * Reads a specific image palette from file.
     *
     * A scalar dataset may have multiple palettes attached to it. readPalette(int idx) returns a specific palette
     * identified by its index.
     *
     * @param idx
     *            the index of the palette to read.
     *
     * @return the image palette
     */
    public byte[][] readPalette(int idx) {
        return null;
    }

    /**
     * Get the name of a specific image palette from file.
     *
     * A scalar dataset may have multiple palettes attached to it. getPaletteName(int idx) returns the name of a
     * specific palette identified by its index.
     *
     * @param idx
     *            the index of the palette to retrieve the name.
     *
     * @return The name of the palette
     */
    public String getPaletteName(int idx) {
        String paletteName = "Default ";
        if (idx != 0)
            paletteName = "Default " + idx;
        return paletteName;
    }

    /**
     * Get the number of pallettes for this object.
     *
     * @return the number of palettes if it has any,
     *         0 if there is no palette attribute attached to this dataset.
     */
    public int getNumberOfPalettes() {
        return 0;
    }

    /**
     * Returns true if this dataset is an image.
     *
     * For all Images, they must have an attribute called "CLASS". The value of this attribute is "IMAGE". For more
     * details, read <a href="https://hdfgroup.github.io/hdf5/_i_m_g.html"> HDF5 Image and Palette Specification</a>
     *
     * @return true if the dataset is an image; otherwise, returns false.
     */
    public final boolean isImage() {
        return isImage;
    }

    /**
     * Returns true if this dataset is displayed as an image.
     *
     * A ScalarDS can be displayed as an image or a spreadsheet in a table.
     *
     * @return true if this dataset is displayed as an image; otherwise, returns false.
     */
    public final boolean isImageDisplay() {

        return isImageDisplay;
    }

    /**
     * Returns true if this dataset is displayed as an image with default image order.
     *
     * A ScalarDS can be displayed as an image with different orders of dimensions.
     *
     * @return true if this dataset is displayed as an image with default image order; otherwise, returns false.
     */
    public final boolean isDefaultImageOrder() {
        return isDefaultImageOrder;
    }

    /**
     * Sets the flag to display the dataset as an image.
     *
     * @param b
     *            if b is true, display the dataset as an image
     */
    public final void setIsImageDisplay(boolean b) {
        isImageDisplay = b;
    }

    /**
     * Sets the flag to indicate this dataset is an image.
     *
     * @param b
     *            if b is true, the dataset is an image.
     */
    public final void setIsImage(boolean b) {
        isImage = b;
    }

    /**
     * Sets data range for an image.
     *
     * @param min
     *            the data range start.
     * @param max
     *            the data range end.
     */
    public final void setImageDataRange(double min, double max) {
        if (max <= min)
            return;

        if (imageDataRange == null)
            imageDataRange = new double[2];

        imageDataRange[0] = min;
        imageDataRange[1] = max;
    }

    /**
     * Add a value that will be filtered out in an image.
     *
     * @param x
     *            value to be filtered
     */
    public void addFilteredImageValue(Number x) {
        Iterator<Number> it = filteredImageValues.iterator();
        while (it.hasNext()) {
            if (it.next().toString().equals(x.toString()))
                return;
        }

        filteredImageValues.add(x);
    }

    /**
     * Get a list of values that will be filtered out in an image.
     *
     * @return the list of Image values
     */
    public List<Number> getFilteredImageValues() {
        return filteredImageValues;
    }

    /**
     * @return true if this dataset is a true color image.
     *
     */

    public final boolean isTrueColor() {
        return isTrueColor;
    }

    /**
     * Returns the interlace mode of a true color image (RGB).
     *
     * Valid values:
     *
     * <pre>
     *     INTERLACE_PIXEL -- RGB components are contiguous, i.e. rgb, rgb, rgb, ...
     *     INTERLACE_LINE -- each RGB component is stored as a scan line
     *     INTERLACE_PLANE -- each RGB component is stored as a plane
     * </pre>
     *
     * @return the interlace mode of a true color image (RGB).
     */
    public final int getInterlace() {
        return interlace;
    }

    /**
     * Returns the (min, max) pair of image data range.
     *
     * @return the (min, max) pair of image data range.
     */
    public double[] getImageDataRange() {
        return imageDataRange;
    }

    /**
     * Returns the fill values for the dataset.
     *
     * @return the fill values for the dataset.
     */
    @Override
    public final Object getFillValue() {
        return fillValue;
    }
}
