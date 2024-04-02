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

package hdf.view;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;

import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.ScalarDS;
import hdf.view.ViewProperties.BITMASK_OP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * The "Tools" class contains various tools for HDF files such as jpeg to HDF
 * converter.
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public final class Tools {
    private static final Display display = Display.getDefault();

    private static final Logger log = LoggerFactory.getLogger(Tools.class);

    /** Maximum value or int8 */
    public static final long MAX_INT8 = 127;
    /** Maximum value or unsigned int8 */
    public static final long MAX_UINT8 = 255;
    /** Maximum value or int16 */
    public static final long MAX_INT16 = 32767;
    /** Maximum value or unsigned int16 */
    public static final long MAX_UINT16 = 65535;
    /** Maximum value or int32 */
    public static final long MAX_INT32 = 2147483647;
    /** Maximum value or unsigned int32 */
    public static final long MAX_UINT32 = 4294967295L;
    /** Maximum value or int64 */
    public static final long MAX_INT64 = 9223372036854775807L;
    /** Maximum value or unsigned int64 */
    public static final BigInteger MAX_UINT64 = new BigInteger("18446744073709551615");

    private static final int FLOAT_BUFFER_SIZE  = 524288;
    private static final int INT_BUFFER_SIZE    = 524288;
    private static final int SHORT_BUFFER_SIZE  = 1048576;
    private static final int LONG_BUFFER_SIZE   = 262144;
    private static final int DOUBLE_BUFFER_SIZE = 262144;
    private static final int BYTE_BUFFER_SIZE   = 2097152;

    /** Key for JPEG image file type. */
    public static final String FILE_TYPE_JPEG = "JPEG";

    /** Key for TIFF image file type. */
    public static final String FILE_TYPE_TIFF = "TIFF";

    /** Key for PNG image file type. */
    public static final String FILE_TYPE_PNG = "PNG";

    /** Key for GIF image file type. */
    public static final String FILE_TYPE_GIF = "GIF";

    /** Key for BMP image file type. */
    public static final String FILE_TYPE_BMP = "BMP";

    /** Key for all image file type. */
    public static final String FILE_TYPE_IMAGE = "IMG";

    /**
     * Converts unsigned 64-bit integer data to a BigInteger since Java does not
     * have unsigned types.
     *
     * @param l
     *        The long value to convert to a BigInteger
     *
     * @return A BigInteger representing the unsigned value of the given long.
     */
    public static BigInteger convertUINT64toBigInt(Long l)
    {
        if (l < 0) {
            l               = (l << 1) >>> 1;
            BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
            BigInteger big2 = new BigInteger(l.toString());
            return big1.add(big2);
        }
        else {
            return new BigInteger(l.toString());
        }
    }

    /**
     * Converts an image file into HDF4/5 file.
     *
     * @param imgFileName
     *            the input image file.
     * @param hFileName
     *            the name of the HDF4/5 file.
     * @param fromType
     *            the type of image.
     * @param toType
     *            the type of file converted to.
     *
     * @throws Exception if a failure occurred
     */
    public static void convertImageToHDF(String imgFileName, String hFileName, String fromType, String toType)
        throws Exception
    {
        File imgFile = null;

        if (imgFileName == null)
            throw new NullPointerException("The source image file is null.");

        imgFile = new File(imgFileName);
        if (!imgFile.exists())
            throw new NullPointerException("The source image file does not exist.");
        if (hFileName == null)
            throw new NullPointerException("The target HDF file is null.");

        if (!fromType.equals(FILE_TYPE_IMAGE))
            throw new UnsupportedOperationException("Unsupported image type.");
        else if (!(toType.equals(FileFormat.FILE_TYPE_HDF4) || toType.equals(FileFormat.FILE_TYPE_HDF5)))
            throw new UnsupportedOperationException("Unsupported destination file type.");

        BufferedImage image = null;
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(imgFileName));
            image                  = ImageIO.read(in);
            in.close();
        }
        catch (Exception err) {
            image = null;
        }

        if (image == null)
            throw new UnsupportedOperationException("Failed to read image: " + imgFileName);

        long h      = image.getHeight();
        long w      = image.getWidth();
        byte[] data = null;

        try {
            data = new byte[(int)(3 * h * w)];
        }
        catch (OutOfMemoryError err) {
            err.printStackTrace();
            throw err;
        }

        int idx = 0;
        int rgb = 0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                rgb         = image.getRGB(j, i);
                data[idx++] = (byte)(rgb >> 16);
                data[idx++] = (byte)(rgb >> 8);
                data[idx++] = (byte)rgb;
            }
        }

        long[] dims        = null;
        Datatype type      = null;
        Group pgroup       = null;
        String imgName     = imgFile.getName();
        FileFormat newfile = null;
        FileFormat thefile = null;
        if (toType.equals(FileFormat.FILE_TYPE_HDF5)) {
            thefile       = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            long[] h5dims = {h, w, 3}; // RGB pixel interlace
            dims          = h5dims;
        }
        else if (toType.equals(FileFormat.FILE_TYPE_HDF4)) {
            thefile       = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);
            long[] h4dims = {w, h, 3}; // RGB pixel interlace
            dims          = h4dims;
        }
        else {
            thefile = null;
        }

        if (thefile != null) {
            newfile = thefile.createInstance(hFileName, FileFormat.CREATE);
            newfile.open();
            pgroup = (Group)newfile.getRootObject();
            type   = newfile.createDatatype(Datatype.CLASS_CHAR, 1, Datatype.NATIVE, Datatype.SIGN_NONE);
            newfile.createImage(imgName, pgroup, type, dims, null, null, -1, 3, ScalarDS.INTERLACE_PIXEL,
                                data);
            newfile.close();
        }

        Runtime.getRuntime().gc();
    }

    /**
     * Save a BufferedImage into an image file.
     *
     * @param image
     *            the BufferedImage to save.
     * @param file
     *            the image file.
     * @param type
     *            the image type.
     *
     * @throws IOException
     *             if a failure occurred
     */
    public static void saveImageAs(BufferedImage image, File file, String type) throws IOException
    {
        if (image == null)
            throw new NullPointerException("The source image is null.");

        ImageIO.write(image, type, file);
    }

    /**
     * Creates the gray palette of the indexed 256-color table.
     *
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the gray palette in the form of byte[3][256]
     */
    public static final byte[][] createGrayPalette()
    {
        byte[][] p = new byte[3][256];

        for (int i = 0; i < 256; i++)
            p[0][i] = p[1][i] = p[2][i] = (byte)(i);

        return p;
    }

    /**
     * Creates the reverse gray palette of the indexed 256-color table.
     *
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the gray palette in the form of byte[3][256]
     */
    public static final byte[][] createReverseGrayPalette()
    {
        byte[][] p = new byte[3][256];

        for (int i = 0; i < 256; i++)
            p[0][i] = p[1][i] = p[2][i] = (byte)(255 - i);

        return p;
    }

    /**
     * Creates the gray wave palette of the indexed 256-color table.
     *
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the gray palette in the form of byte[3][256]
     */
    public static final byte[][] createGrayWavePalette()
    {
        byte[][] p = new byte[3][256];

        for (int i = 0; i < 256; i++)
            p[0][i] = p[1][i] = p[2][i] =
                (byte)((double)255 / 2 + ((double)255 / 2) * Math.sin((i - 32) / 20.3));

        return p;
    }

    /**
     * Creates the rainbow palette of the indexed 256-color table.
     *
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the rainbow palette in the form of byte[3][256]
     */
    public static final byte[][] createRainbowPalette()
    {
        byte r;
        byte g;
        byte b;
        byte[][] p = new byte[3][256];

        for (int i = 1; i < 255; i++) {
            if (i <= 29) {
                r = (byte)(129.36 - i * 4.36);
                g = 0;
                b = (byte)255;
            }
            else if (i <= 86) {
                r = 0;
                g = (byte)(-133.54 + i * 4.52);
                b = (byte)255;
            }
            else if (i <= 141) {
                r = 0;
                g = (byte)255;
                b = (byte)(665.83 - i * 4.72);
            }
            else if (i <= 199) {
                r = (byte)(-635.26 + i * 4.47);
                g = (byte)255;
                b = 0;
            }
            else {
                r = (byte)255;
                g = (byte)(1166.81 - i * 4.57);
                b = 0;
            }

            p[0][i] = r;
            p[1][i] = g;
            p[2][i] = b;
        }

        p[0][0] = p[1][0] = p[2][0] = 0;
        p[0][255] = p[1][255] = p[2][255] = (byte)255;

        return p;
    }

    /**
     * Creates the nature palette of the indexed 256-color table.
     *
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the nature palette in the form of byte[3][256]
     */
    public static final byte[][] createNaturePalette()
    {
        byte[][] p = new byte[3][256];

        for (int i = 1; i < 210; i++) {
            p[0][i] = (byte)((Math.sin((double)(i - 5) / 16) + 1) * 90);
            p[1][i] = (byte)((1 - Math.sin((double)(i - 30) / 12)) * 64 * (1 - (double)i / 255) + 128 -
                             (double)i / 2);
            p[2][i] = (byte)((1 - Math.sin((double)(i - 8) / 9)) * 110 + 30);
        }

        for (int i = 210; i < 255; i++) {
            p[0][i] = (byte)80;
            p[1][i] = (byte)0;
            p[2][i] = (byte)200;
        }

        p[0][0] = p[1][0] = p[2][0] = 0;
        p[0][255] = p[1][255] = p[2][255] = (byte)255;

        return p;
    }

    /**
     * Creates the wave palette of the indexed 256-color table.
     *
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the wave palette in the form of byte[3][256]
     */
    public static final byte[][] createWavePalette()
    {
        byte[][] p = new byte[3][256];

        for (int i = 1; i < 255; i++) {
            p[0][i] = (byte)((Math.sin(((double)i / 40 - 3.2)) + 1) * 128);
            p[1][i] = (byte)((1 - Math.sin((i / 2.55 - 3.1))) * 70 + 30);
            p[2][i] = (byte)((1 - Math.sin(((double)i / 40 - 3.1))) * 128);
        }

        p[0][0] = p[1][0] = p[2][0] = 0;
        p[0][255] = p[1][255] = p[2][255] = (byte)255;

        return p;
    }

    /**
     * read an image palette from a file.
     *
     * A palette file has format of (value, red, green, blue). The color value
     * in palette file can be either unsigned char [0..255] or float [0..1].
     * Float value will be converted to [0..255].
     *
     * The color table in file can have any number of entries between 2 to 256.
     * It will be converted to a color table of 256 entries. Any missing index
     * will calculated by linear interpolation between the neighboring index
     * values. For example, index 11 is missing in the following table 10 200 60
     * 20 12 100 100 60 Index 11 will be calculated based on index 10 and index
     * 12, i.e. 11 150 80 40
     *
     * @param filename
     *            the name of the palette file.
     *
     * @return the wave palette in the form of byte[3][256]
     */
    public static final byte[][] readPalette(String filename)
    {
        final int COLOR256 = 256;
        int nentries       = 0;
        int i              = 0;
        int j              = 0;
        int idx            = 0;
        float v            = 0;
        float r            = 0;
        float g            = 0;
        float b            = 0;
        float ratio        = 0;
        float maxV         = 0;
        float minV         = 0;
        float maxColor     = 0;
        float minColor     = 0;
        float[][] tbl      = new float[COLOR256][4]; /* value, red, green, blue */

        if (filename == null)
            return null;

        try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
            String line = null;
            do {
                try {
                    line = in.readLine();
                }
                catch (Exception ex) {
                    log.debug("input file:", ex);
                    line = null;
                }

                if (line == null)
                    continue;

                StringTokenizer st = new StringTokenizer(line);

                // invalid line
                if (st.countTokens() != 4) {
                    continue;
                }

                try {
                    v = Float.valueOf(st.nextToken());
                    r = Float.valueOf(st.nextToken());
                    g = Float.valueOf(st.nextToken());
                    b = Float.valueOf(st.nextToken());
                }
                catch (NumberFormatException ex) {
                    log.debug("input file:", ex);
                    continue;
                }

                tbl[idx][0] = v;
                tbl[idx][1] = r;
                tbl[idx][2] = g;
                tbl[idx][3] = b;

                if (idx == 0) {
                    maxV = minV = v;
                    maxColor = minColor = r;
                }

                maxV     = Math.max(maxV, v);
                maxColor = Math.max(maxColor, r);
                maxColor = Math.max(maxColor, g);
                maxColor = Math.max(maxColor, b);

                minV     = Math.min(minV, v);
                minColor = Math.min(minColor, r);
                minColor = Math.min(minColor, g);
                minColor = Math.min(minColor, b);

                idx++;
                if (idx >= COLOR256)
                    break; /* only support to 256 colors */
            } while (line != null);
        }
        catch (Exception ex) {
            log.debug("input file:", ex);
        }

        nentries = idx;
        if (nentries <= 1) // must have more than one entries
            return null;

        // convert color table to byte
        nentries = idx;
        if (maxColor <= 1) {
            ratio = (minColor == maxColor) ? 1.0f : ((COLOR256 - 1.0f) / (maxColor - minColor));

            for (i = 0; i < nentries; i++) {
                for (j = 1; j < 4; j++)
                    tbl[i][j] = (tbl[i][j] - minColor) * ratio;
            }
        }

        // convert table to 256 entries
        idx   = 0;
        ratio = (minV == maxV) ? 1.0f : ((COLOR256 - 1.0f) / (maxV - minV));

        int[][] p = new int[3][COLOR256];
        for (i = 0; i < nentries; i++) {
            idx = (int)((tbl[i][0] - minV) * ratio);
            for (j = 0; j < 3; j++)
                p[j][idx] = (int)tbl[i][j + 1];
        }

        /* linear interpolating missing values in the color table */
        for (i = 1; i < COLOR256; i++) {
            if ((p[0][i] + p[1][i] + p[2][i]) == 0) {
                j = i + 1;

                // figure out number of missing points between two given points
                while (j < COLOR256 && (p[0][j] + p[1][j] + p[2][j]) == 0)
                    j++;

                if (j >= COLOR256)
                    break; // nothing in the table to interpolating

                float d1 = (p[0][j] - p[0][i - 1]) / (float)(j - i);
                float d2 = (p[1][j] - p[1][i - 1]) / (float)(j - i);
                float d3 = (p[2][j] - p[2][i - 1]) / (float)(j - i);

                for (int k = i; k <= j; k++) {
                    p[0][k] = (int)(p[0][i - 1] + d1 * (k - i + 1));
                    p[1][k] = (int)(p[1][i - 1] + d2 * (k - i + 1));
                    p[2][k] = (int)(p[2][i - 1] + d3 * (k - i + 1));
                }
                i = j + 1;
            } // ((p[0][i] + p[1][i] + p[2][i]) == 0)
        }     // (i = 1; i < COLOR256; i++)

        byte[][] pal = new byte[3][COLOR256];
        for (i = 1; i < COLOR256; i++) {
            for (j = 0; j < 3; j++)
                pal[j][i] = (byte)(p[j][i]);
        }

        return pal;
    }

    /**
     * This method returns true if the specified image has transparent pixels.
     *
     * @param image
     *            the image to be check if has alpha.
     *
     * @return true if the image has alpha setting.
     */
    public static boolean hasAlpha(Image image)
    {
        if (image == null)
            return false;

        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage)image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        }
        catch (InterruptedException e) {
            log.debug("transparent pixels:", e);
        }
        ColorModel cm = pg.getColorModel();

        return cm.hasAlpha();
    }

    /**
     * Creates a RGB indexed image of 256 colors.
     *
     * @param bufferedImage
     *            the target image.
     * @param imageData
     *            the byte array of the image data.
     * @param palette
     *            the color lookup table.
     * @param w
     *            the width of the image.
     * @param h
     *            the height of the image.
     *
     * @return the image.
     */
    public static Image createIndexedImage(BufferedImage bufferedImage, byte[] imageData, byte[][] palette,
                                           long w, long h)
    {
        if (imageData == null || w <= 0 || h <= 0)
            return null;

        if (palette == null)
            palette = Tools.createGrayPalette();

        if (bufferedImage == null)
            bufferedImage = new BufferedImage((int)w, (int)h, BufferedImage.TYPE_INT_ARGB);

        final int[] pixels = ((DataBufferInt)bufferedImage.getRaster().getDataBuffer()).getData();
        int len            = pixels.length;

        for (int i = 0; i < len; i++) {
            int idx = imageData[i] & 0xff;
            int r   = (palette[0][idx] & 0xff) << 16;
            int g   = (palette[1][idx] & 0xff) << 8;
            int b   = palette[2][idx] & 0xff;

            pixels[i] = 0xff000000 | r | g | b;
        }

        return bufferedImage;
    }

    /**
     * Creates a true color image.
     *
     * DirectColorModel is used to construct the image from raw data. The DirectColorModel model is
     * similar to an X11 TrueColor visual, which has the following parameters: <br>
     *
     * <pre>
     * Number of bits:        32
     *             Red mask:              0x00ff0000
     *             Green mask:            0x0000ff00
     *             Blue mask:             0x000000ff
     *             Alpha mask:            0xff000000
     *             Color space:           sRGB
     *             isAlphaPremultiplied:  False
     *             Transparency:          Transparency.TRANSLUCENT
     *             transferType:          DataBuffer.TYPE_INT
     * </pre>
     *
     * The data may be arranged in one of two ways: by pixel or by plane. In both cases, the dataset
     * will have a dataspace with three dimensions, height, width, and components.
     *
     * For HDF4, the interlace modes specify orders for the dimensions as:
     *
     * <pre>
     * INTERLACE_PIXEL = [width][height][pixel components]
     *            INTERLACE_PLANE = [pixel components][width][height]
     * </pre>
     *
     * For HDF5, the interlace modes specify orders for the dimensions as:
     *
     * <pre>
     * INTERLACE_PIXEL = [height][width][pixel components]
     *            INTERLACE_PLANE = [pixel components][height][width]
     * </pre>
     *
     * @param imageData
     *            the byte array of the image data.
     * @param planeInterlace
     *            flag if the image is plane interlace.
     * @param w
     *            the width of the image.
     * @param h
     *            the height of the image.
     *
     * @return the image.
     */
    public static Image createTrueColorImage(byte[] imageData, boolean planeInterlace, long w, long h)
    {
        Image theImage        = null;
        long imgSize          = w * h;
        int[] packedImageData = new int[(int)imgSize];
        int pixel             = 0;
        int idx               = 0;
        int r                 = 0;
        int g                 = 0;
        int b                 = 0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                if (planeInterlace) {
                    r = imageData[idx];
                    g = imageData[(int)imgSize + idx];
                    b = imageData[(int)imgSize * 2 + idx];
                }
                else {
                    r = imageData[idx * 3];
                    g = imageData[idx * 3 + 1];
                    b = imageData[idx * 3 + 2];
                }

                r = (r << 16) & 0x00ff0000;
                g = (g << 8) & 0x0000ff00;
                b = b & 0x000000ff;

                // bits packed into alpha (1), red (r), green (g) and blue (b)
                // as 11111111rrrrrrrrggggggggbbbbbbbb
                pixel                  = 0xff000000 | r | g | b;
                packedImageData[idx++] = pixel;
            } // (int j=0; j<w; j++)
        }     // (int i=0; i<h; i++)

        DirectColorModel dcm = (DirectColorModel)ColorModel.getRGBdefault();
        theImage             = Toolkit.getDefaultToolkit().createImage(
                        new MemoryImageSource((int)w, (int)h, dcm, packedImageData, 0, (int)w));

        return theImage;
    }

    /**
     * This method returns a buffered image with the contents of an image.
     *
     * @param image
     *            the plain image object.
     *
     * @return buffered image for the given image.
     */
    public static BufferedImage toBufferedImage(Image image)
    {
        if (image == null)
            return null;

        if (image instanceof BufferedImage)
            return (BufferedImage)image;

        // !!!!!!!!!!!!!!!!!! NOTICE !!!!!!!!!!!!!!!!!!!!!
        // the following way of creating a buffered image is using
        // Component.createImage(). This method can be used only if the
        // component is visible on the screen. Also, this method returns
        // buffered images that do not support transparent pixels.
        // The buffered image created by this way works for package
        // com.sun.image.codec.jpeg.*
        // It does not work well with JavaTM Advanced Imaging
        // com.sun.media.jai.codec.*
        // if the screen setting is less than 32-bit color
        int w                = image.getWidth(null);
        int h                = image.getHeight(null);
        BufferedImage bimage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g           = bimage.createGraphics();
        g.drawImage(image, 0, 0, null);

        g.dispose();
        return bimage;
    }

    /**
     * Convert an array of raw data into array of a byte data.
     *
     * @param rawData
     *            The input raw data.
     * @param minmax
     *            the range of the raw data.
     * @param w
     *            the width of the raw data.
     * @param h
     *            the height of the raw data.
     * @param isTransposed
     *            if the data is transposed.
     * @param byteData
     *            the data in.
     *
     * @return the byte array of pixel data.
     */
    public static byte[] getBytes(Object rawData, double[] minmax, long w, long h, boolean isTransposed,
                                  byte[] byteData)
    {
        return Tools.getBytes(rawData, minmax, w, h, isTransposed, null, false, byteData);
    }

    /**
     * Convert an array of raw data into array of a byte data.
     *
     * @param rawData
     *            The input raw data.
     * @param minmax
     *            the range of the raw data.
     * @param w
     *            the width of the raw data.
     * @param h
     *            the height of the raw data.
     * @param isTransposed
     *            if the data is transposed.
     * @param invalidValues
     *            list of values out of range.
     * @param byteData
     *            the data in.
     *
     * @return the byte array of pixel data.
     */
    public static byte[] getBytes(Object rawData, double[] minmax, long w, long h, boolean isTransposed,
                                  List<Number> invalidValues, byte[] byteData)
    {
        return getBytes(rawData, minmax, w, h, isTransposed, invalidValues, false, byteData);
    }

    /**
     * Convert an array of raw data into array of a byte data.
     *
     * @param rawData
     *            The input raw data.
     * @param minmax
     *            the range of the raw data.
     * @param w
     *            the width of the raw data.
     * @param h
     *            the height of the raw data.
     * @param isTransposed
     *            if the data is transposed.
     * @param invalidValues
     *            list of values out of range.
     * @param convertByteData
     *            the converted data out.
     * @param byteData
     *            the data in.
     *
     * @return the byte array of pixel data.
     */
    public static byte[] getBytes(Object rawData, double[] minmax, long w, long h, boolean isTransposed,
                                  List<Number> invalidValues, boolean convertByteData, byte[] byteData)
    {
        return getBytes(rawData, minmax, w, h, isTransposed, invalidValues, convertByteData, byteData, null);
    }

    /**
     * Convert an array of raw data into array of a byte data.
     *
     * @param rawData
     *            The input raw data.
     * @param minmax
     *            the range of the raw data.
     * @param w
     *            the width of the raw data.
     * @param h
     *            the height of the raw data.
     * @param isTransposed
     *            if the data is transposed.
     * @param invalidValues
     *            the list of invalid values.
     * @param convertByteData
     *            the converted data out.
     * @param byteData
     *            the data in.
     * @param list
     *            the list of integers.
     *
     * @return the byte array of pixel data.
     */
    public static byte[] getBytes(Object rawData, double[] minmax, long w, long h, boolean isTransposed,
                                  List<Number> invalidValues, boolean convertByteData, byte[] byteData,
                                  List<Integer> list)
    {
        double[] fillValue = null;

        // no input data
        if (rawData == null || w <= 0 || h <= 0)
            return null;

        // input data is not an array
        if (!rawData.getClass().isArray())
            return null;

        String cname = rawData.getClass().getName();
        char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
        int size     = Array.getLength(rawData);

        if (minmax == null) {
            minmax    = new double[2];
            minmax[0] = minmax[1] = 0;
        }

        if (dname == 'B')
            return convertByteData((byte[])rawData, minmax, w, h, isTransposed, fillValue, convertByteData,
                                   byteData, list);

        if ((byteData == null) || (size != byteData.length))
            byteData = new byte[size]; // reuse the old buffer

        if (minmax[0] == minmax[1])
            Tools.findMinMax(rawData, minmax, fillValue);

        double min = minmax[0];
        double max = minmax[1];

        if (invalidValues != null && !invalidValues.isEmpty()) {
            int n     = invalidValues.size();
            fillValue = new double[n];
            for (int i = 0; i < n; i++) {
                fillValue[i] = invalidValues.get(i).doubleValue();
            }
        }
        double ratio = (min == max) ? 1.00d : (double)(255.00 / (max - min));
        long idxSrc  = 0;
        long idxDst  = 0;
        switch (dname) {
        case 'S':
            short[] s = (short[])rawData;
            for (long i = 0; i < h; i++) {
                for (long j = 0; j < w; j++) {
                    idxSrc = idxDst = j * h + i;
                    if (isTransposed)
                        idxDst = i * w + j;
                    byteData[(int)idxDst] =
                        toByte(s[(int)idxSrc], ratio, min, max, fillValue, (int)idxSrc, list);
                }
            }
            break;

        case 'I':
            int[] ia = (int[])rawData;
            for (long i = 0; i < h; i++) {
                for (long j = 0; j < w; j++) {
                    idxSrc = idxDst = (j * h + i);
                    if (isTransposed)
                        idxDst = i * w + j;
                    byteData[(int)idxDst] =
                        toByte(ia[(int)idxSrc], ratio, min, max, fillValue, (int)idxSrc, list);
                }
            }
            break;

        case 'J':
            long[] l = (long[])rawData;
            for (long i = 0; i < h; i++) {
                for (long j = 0; j < w; j++) {
                    idxSrc = idxDst = j * h + i;
                    if (isTransposed)
                        idxDst = i * w + j;
                    byteData[(int)idxDst] =
                        toByte(l[(int)idxSrc], ratio, min, max, fillValue, (int)idxSrc, list);
                }
            }
            break;

        case 'F':
            float[] f = (float[])rawData;
            for (long i = 0; i < h; i++) {
                for (long j = 0; j < w; j++) {
                    idxSrc = idxDst = j * h + i;
                    if (isTransposed)
                        idxDst = i * w + j;
                    byteData[(int)idxDst] =
                        toByte(f[(int)idxSrc], ratio, min, max, fillValue, (int)idxSrc, list);
                }
            }
            break;

        case 'D':
            double[] d = (double[])rawData;
            for (long i = 0; i < h; i++) {
                for (long j = 0; j < w; j++) {
                    idxSrc = idxDst = j * h + i;
                    if (isTransposed)
                        idxDst = i * w + j;
                    byteData[(int)idxDst] =
                        toByte(d[(int)idxSrc], ratio, min, max, fillValue, (int)idxSrc, list);
                }
            }
            break;

        default:
            byteData = null;
            break;
        } // (dname)

        return byteData;
    }

    private static byte toByte(double in, double ratio, double min, double max, double[] fill, int idx,
                               List<Integer> list)
    {
        byte out = 0;

        if (in < min || in > max || isFillValue(in, fill) || isNaNINF(in)) {
            out = 0;
            if (list != null)
                list.add(idx);
        }
        else
            out = (byte)((in - min) * ratio);

        return out;
    }

    private static boolean isFillValue(double in, double[] fill)
    {
        if (fill == null)
            return false;

        for (int i = 0; i < fill.length; i++) {
            if (fill[i] == in)
                return true;
        }

        return false;
    }

    private static byte[] convertByteData(byte[] rawData, double[] minmax, long w, long h,
                                          boolean isTransposed, Object fillValue, boolean convertByteData,
                                          byte[] byteData, List<Integer> list)
    {
        if (rawData == null)
            return null;

        if (convertByteData) {
            if (minmax[0] == minmax[1])
                Tools.findMinMax(rawData, minmax, fillValue);
        }

        if (minmax[0] == 0 && minmax[1] == 255)
            convertByteData = false; // no need to convert data

        // no conversion and no transpose
        if (!convertByteData && !isTransposed) {
            if (byteData != null && byteData.length == rawData.length) {
                System.arraycopy(rawData, 0, byteData, 0, rawData.length);
                return byteData;
            }

            return rawData;
        }

        // don't want to change the original raw data
        if (byteData == null || rawData == byteData)
            byteData = new byte[rawData.length];

        if (!convertByteData) {
            // do not convert data, just transpose the data
            minmax[0] = 0;
            minmax[1] = 255;
            if (isTransposed) {
                for (long i = 0; i < h; i++) {
                    for (long j = 0; j < w; j++)
                        byteData[(int)(i * w + j)] = rawData[(int)(j * h + i)];
                }
            }
            return byteData;
        }

        // special data range used, must convert the data
        double min   = minmax[0];
        double max   = minmax[1];
        double ratio = (min == max) ? 1.00d : (double)(255.00 / (max - min));
        long idxSrc  = 0;
        long idxDst  = 0;
        for (long i = 0; i < h; i++) {
            for (long j = 0; j < w; j++) {
                idxSrc = idxDst = j * h + i;
                if (isTransposed)
                    idxDst = i * w + j;

                if (rawData[(int)idxSrc] > max || rawData[(int)idxSrc] < min) {
                    byteData[(int)idxDst] = (byte)0;
                    if (list != null)
                        list.add((int)idxSrc);
                }
                else
                    byteData[(int)idxDst] = (byte)((rawData[(int)idxSrc] - min) * ratio);
            }
        }

        return byteData;
    }

    /**
     * Create and initialize a new instance of the given class.
     *
     * @param cls
     *           the class of the instance
     * @param initargs
     *            array of objects to be passed as arguments.
     *
     * @return a new instance of the given class.
     *
     * @throws Exception if a failure occurred
     */
    public static Object newInstance(Class<?> cls, Object[] initargs) throws Exception
    {
        log.trace("newInstance(Class = {}): start", cls);

        if (cls == null)
            return null;

        Object instance = null;

        if ((initargs == null) || (initargs.length == 0)) {
            instance = cls.getDeclaredConstructor().newInstance();
        }
        else {
            Constructor<?>[] constructors = cls.getConstructors();
            if ((constructors == null) || (constructors.length == 0))
                return null;

            boolean isConstructorMatched = false;
            Constructor<?> constructor   = null;
            Class<?>[] params            = null;
            int m                        = constructors.length;
            int n                        = initargs.length;
            for (int i = 0; i < m; i++) {
                constructor = constructors[i];
                params      = constructor.getParameterTypes();
                if (params.length == n) {
                    // check if all the parameters are matched
                    isConstructorMatched = params[0].isInstance(initargs[0]);
                    for (int j = 0; j < n; j++)
                        isConstructorMatched = isConstructorMatched && params[j].isInstance(initargs[j]);

                    if (isConstructorMatched) {
                        try {
                            instance = constructor.newInstance(initargs);
                        }
                        catch (Exception ex) {
                            log.debug("Error creating instance of {}: ", cls, ex);
                            ex.printStackTrace();
                        }
                        break;
                    }
                }
            } // (int i=0; i<m; i++)
        }

        return instance;
    }

    /**
     * Computes autocontrast parameters (gain equates to contrast and bias
     * equates to brightness) for integers.
     *
     * The computation is based on the following scaling
     *
     * <pre>
     *      int_8       [0, 127]
     *      uint_8      [0, 255]
     *      int_16      [0, 32767]
     *      uint_16     [0, 65535]
     *      int_32      [0, 2147483647]
     *      uint_32     [0, 4294967295]
     *      int_64      [0, 9223372036854775807]
     *      uint_64     [0, 18446744073709551615] // Not supported.
     * </pre>
     *
     * @param data
     *            the raw data array of signed/unsigned integers
     * @param params
     *            the auto gain parameter. params[0]=gain, params[1]=bias,
     * @param isUnsigned
     *            the flag to indicate if the data array is unsigned integer.
     *
     * @return non-negative if successful; otherwise, returns negative
     */
    public static int autoContrastCompute(Object data, double[] params, boolean isUnsigned)
    {
        int retval        = 1;
        long maxDataValue = 255;
        double[] minmax   = new double[2];

        // check parameters
        if ((data == null) || (params == null) || (Array.getLength(data) <= 0) || (params.length < 2))
            return -1;

        retval = autoContrastComputeMinMax(data, minmax);

        // force the min_max method so we can look at the target grids data sets
        if ((retval < 0) || (minmax[1] - minmax[0] < 10))
            retval = findMinMax(data, minmax, null);

        if (retval < 0)
            return -1;

        String cname = data.getClass().getName();
        char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
        switch (dname) {
        case 'B':
            maxDataValue = MAX_INT8;
            break;
        case 'S':
            maxDataValue = MAX_INT16;
            if (isUnsigned)
                maxDataValue = MAX_UINT8; // data was upgraded from unsigned byte
            break;
        case 'I':
            maxDataValue = MAX_INT32;
            if (isUnsigned)
                maxDataValue = MAX_UINT16; // data was upgraded from unsigned short
            break;
        case 'J':
            maxDataValue = MAX_INT64;
            if (isUnsigned)
                maxDataValue = MAX_UINT32; // data was upgraded from unsigned int
            break;
        default:
            retval = -1;
            break;
        } // (dname)

        if (minmax[0] == minmax[1]) {
            params[0] = 1.0;
            params[1] = 0.0;
        }
        else {
            // This histogram method has a tendency to stretch the
            // range of values to be a bit too big, so we can
            // account for this by adding and subtracting some percent
            // of the difference to the max/min values
            // to prevent the gain from going too high.
            double diff   = minmax[1] - minmax[0];
            double newmax = (minmax[1] + (diff * 0.1));
            double newmin = (minmax[0] - (diff * 0.1));

            if (newmax <= maxDataValue)
                minmax[1] = newmax;

            if (newmin >= 0)
                minmax[0] = newmin;

            params[0] = maxDataValue / (minmax[1] - minmax[0]);
            params[1] = -minmax[0];
        }

        return retval;
    }

    /**
     * Apply autocontrast parameters to the original data in place (destructive)
     *
     * @param dataIN
     *            the original data array of signed/unsigned integers
     * @param dataOUT
     *            the converted data array of signed/unsigned integers
     * @param params
     *            the auto gain parameter. params[0]=gain, params[1]=bias
     * @param minmax
     *            the data range. minmax[0]=min, minmax[1]=max
     * @param isUnsigned
     *            the flag to indicate if the data array is unsigned integer
     *
     * @return the data array with the auto contrast conversion; otherwise,
     *         returns null
     */
    public static Object autoContrastApply(Object dataIN, Object dataOUT, double[] params, double[] minmax,
                                           boolean isUnsigned)
    {
        int size   = 0;
        double min = -MAX_INT64;
        double max = MAX_INT64;

        if ((dataIN == null) || (params == null) || (params.length < 2))
            return null;

        if (minmax != null) {
            min = minmax[0];
            max = minmax[1];
        }
        // input and output array must be the same size
        size = Array.getLength(dataIN);
        if ((dataOUT != null) && (size != Array.getLength(dataOUT)))
            return null;

        double gain = params[0];
        double bias = params[1];
        double valueOut;
        double valueIn;
        String cname = dataIN.getClass().getName();
        char dname   = cname.charAt(cname.lastIndexOf('[') + 1);

        switch (dname) {
        case 'B':
            byte[] bIn = (byte[])dataIN;
            if (dataOUT == null)
                dataOUT = new byte[size];
            byte[] bOut = (byte[])dataOUT;
            byte bMax   = (byte)MAX_INT8;

            for (int i = 0; i < size; i++) {
                valueIn  = Math.max(bIn[i], min);
                valueIn  = Math.min(valueIn, max);
                valueOut = (valueIn + bias) * gain;
                valueOut = Math.max(valueOut, 0.0);
                valueOut = Math.min(valueOut, bMax);
                bOut[i]  = (byte)valueOut;
            }
            break;
        case 'S':
            short[] sIn = (short[])dataIN;
            if (dataOUT == null)
                dataOUT = new short[size];
            short[] sOut = (short[])dataOUT;
            short sMax   = (short)MAX_INT16;

            if (isUnsigned)
                sMax = (short)MAX_UINT8; // data was upgraded from unsigned byte

            for (int i = 0; i < size; i++) {
                valueIn  = Math.max(sIn[i], min);
                valueIn  = Math.min(valueIn, max);
                valueOut = (valueIn + bias) * gain;
                valueOut = Math.max(valueOut, 0.0);
                valueOut = Math.min(valueOut, sMax);
                sOut[i]  = (byte)valueOut;
            }
            break;
        case 'I':
            int[] iIn = (int[])dataIN;
            if (dataOUT == null)
                dataOUT = new int[size];
            int[] iOut = (int[])dataOUT;
            int iMax   = (int)MAX_INT32;
            if (isUnsigned)
                iMax = (int)MAX_UINT16; // data was upgraded from unsigned short

            for (int i = 0; i < size; i++) {
                valueIn  = Math.max(iIn[i], min);
                valueIn  = Math.min(valueIn, max);
                valueOut = (valueIn + bias) * gain;
                valueOut = Math.max(valueOut, 0.0);
                valueOut = Math.min(valueOut, iMax);
                iOut[i]  = (byte)valueOut;
            }
            break;
        case 'J':
            long[] lIn = (long[])dataIN;
            if (dataOUT == null)
                dataOUT = new long[size];
            long[] lOut = (long[])dataOUT;
            long lMax   = MAX_INT64;
            if (isUnsigned)
                lMax = MAX_UINT32; // data was upgraded from unsigned int

            for (int i = 0; i < size; i++) {
                valueIn  = Math.max(lIn[i], min);
                valueIn  = Math.min(valueIn, max);
                valueOut = (valueIn + bias) * gain;
                valueOut = Math.max(valueOut, 0.0);
                valueOut = Math.min(valueOut, lMax);
                lOut[i]  = (byte)valueOut;
            }
            break;
        default:
            break;
        } // (dname)

        return dataOUT;
    }

    /**
     * Converts image raw data to bytes.
     *
     * The integer data is converted to byte data based on the following rule
     *
     * <pre>
     *         uint_8       x
     *         int_8       (x &amp; 0x7F) &lt;&lt; 1
     *         uint_16     (x &gt;&gt; 8) &amp; 0xFF
     *         int_16      (x &gt;&gt; 7) &amp; 0xFF
     *         uint_32     (x &gt;&gt; 24) &amp; 0xFF
     *         int_32      (x &gt;&gt; 23) &amp; 0xFF
     *         uint_64     (x &gt;&gt; 56) &amp; 0xFF
     *         int_64      (x &gt;&gt; 55) &amp; 0xFF
     * </pre>
     *
     * @param src
     *            the source data array of signed integers or unsigned shorts
     * @param dst
     *            the destination data array of bytes
     * @param isUnsigned
     *            the flag to indicate if the data array is unsigned integer.
     *
     * @return non-negative if successful; otherwise, returns negative
     */
    public static int autoContrastConvertImageBuffer(Object src, byte[] dst, boolean isUnsigned)
    {
        int retval = 0;

        if ((src == null) || (dst == null) || (dst.length != Array.getLength(src)))
            return -1;

        int size     = dst.length;
        String cname = src.getClass().getName();
        char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
        switch (dname) {
        case 'B':
            byte[] bSrc = (byte[])src;
            if (isUnsigned) {
                for (int i = 0; i < size; i++)
                    dst[i] = bSrc[i];
            }
            else {
                for (int i = 0; i < size; i++)
                    dst[i] = (byte)((bSrc[i] & 0x7F) << 1);
            }
            break;
        case 'S':
            short[] sSrc = (short[])src;
            if (isUnsigned) { // data was upgraded from unsigned byte
                for (int i = 0; i < size; i++)
                    dst[i] = (byte)sSrc[i];
            }
            else {
                for (int i = 0; i < size; i++)
                    dst[i] = (byte)((sSrc[i] >> 7) & 0xFF);
            }
            break;
        case 'I':
            int[] iSrc = (int[])src;
            if (isUnsigned) { // data was upgraded from unsigned short
                for (int i = 0; i < size; i++)
                    dst[i] = (byte)((iSrc[i] >> 8) & 0xFF);
            }
            else {
                for (int i = 0; i < size; i++)
                    dst[i] = (byte)((iSrc[i] >> 23) & 0xFF);
            }
            break;
        case 'J':
            long[] lSrc = (long[])src;
            if (isUnsigned) { // data was upgraded from unsigned int
                for (int i = 0; i < size; i++)
                    dst[i] = (byte)((lSrc[i] >> 24) & 0xFF);
            }
            else {
                for (int i = 0; i < size; i++)
                    dst[i] = (byte)((lSrc[i] >> 55) & 0xFF);
            }
            break;
        default:
            retval = -1;
            break;
        } // (dname)

        return retval;
    }

    /**
     * Computes autocontrast parameters by
     *
     * <pre>
     *    min = mean - 3 * std.dev
     *    max = mean + 3 * std.dev
     * </pre>
     *
     * @param data
     *            the raw data array
     * @param minmax
     *            the min and max values.
     *
     * @return non-negative if successful; otherwise, returns negative
     */
    public static int autoContrastComputeMinMax(Object data, double[] minmax)
    {
        int retval = 1;

        if ((data == null) || (minmax == null) || (Array.getLength(data) <= 0) ||
            (Array.getLength(minmax) < 2))
            return -1;

        double[] avgstd = {0, 0};
        retval          = computeStatistics(data, avgstd, null);
        if (retval < 0)
            return retval;

        minmax[0] = avgstd[0] - 3.0 * avgstd[1];
        minmax[1] = avgstd[0] + 3.0 * avgstd[1];

        return retval;
    }

    /**
     * Finds the min and max values of the data array
     *
     * @param data
     *            the raw data array
     * @param minmax
     *            the mmin and max values of the array.
     * @param fillValue
     *            the missing value or fill value. Exclude this value when check
     *            for min/max
     *
     * @return non-negative if successful; otherwise, returns negative
     */
    public static int findMinMax(Object data, double[] minmax, Object fillValue)
    {
        int retval = 1;

        if ((data == null) || (minmax == null) || (Array.getLength(data) <= 0) ||
            (Array.getLength(minmax) < 2))
            return -1;

        int n                = Array.getLength(data);
        double fill          = 0.0;
        boolean hasFillValue = (fillValue != null && fillValue.getClass().isArray());

        String cname = data.getClass().getName();
        char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
        log.trace("findMinMax() cname={} : dname={}", cname, dname);

        minmax[0] = Float.MAX_VALUE;
        minmax[1] = -Float.MAX_VALUE;

        switch (dname) {
        case 'B':
            byte[] b  = (byte[])data;
            minmax[0] = minmax[1] = b[0];

            if (hasFillValue)
                fill = ((byte[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if (hasFillValue && b[i] == fill)
                    continue;
                if (minmax[0] > b[i])
                    minmax[0] = b[i];
                if (minmax[1] < b[i])
                    minmax[1] = b[i];
            }
            break;
        case 'S':
            short[] s = (short[])data;
            minmax[0] = minmax[1] = s[0];

            if (hasFillValue)
                fill = ((short[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if (hasFillValue && s[i] == fill)
                    continue;
                if (minmax[0] > s[i])
                    minmax[0] = s[i];
                if (minmax[1] < s[i])
                    minmax[1] = s[i];
            }
            break;
        case 'I':
            int[] ia  = (int[])data;
            minmax[0] = minmax[1] = ia[0];

            if (hasFillValue)
                fill = ((int[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if (hasFillValue && ia[i] == fill)
                    continue;
                if (minmax[0] > ia[i])
                    minmax[0] = ia[i];
                if (minmax[1] < ia[i])
                    minmax[1] = ia[i];
            }
            break;
        case 'J':
            long[] l  = (long[])data;
            minmax[0] = minmax[1] = l[0];

            if (hasFillValue)
                fill = ((long[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if (hasFillValue && l[i] == fill)
                    continue;
                if (minmax[0] > l[i])
                    minmax[0] = l[i];
                if (minmax[1] < l[i])
                    minmax[1] = l[i];
            }
            break;
        case 'F':
            float[] f = (float[])data;
            minmax[0] = minmax[1] = f[0];

            if (hasFillValue)
                fill = ((float[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if ((hasFillValue && f[i] == fill) || isNaNINF(f[i]))
                    continue;
                if (minmax[0] > f[i])
                    minmax[0] = f[i];
                if (minmax[1] < f[i])
                    minmax[1] = f[i];
            }

            break;
        case 'D':
            double[] d = (double[])data;
            minmax[0] = minmax[1] = d[0];

            if (hasFillValue)
                fill = ((double[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if ((hasFillValue && d[i] == fill) || isNaNINF(d[i]))
                    continue;

                if (minmax[0] > d[i])
                    minmax[0] = d[i];
                if (minmax[1] < d[i])
                    minmax[1] = d[i];
            }
            break;
        default:
            retval = -1;
            break;
        } // (dname)

        return retval;
    }

    /**
     * Finds the distribution of data values
     *
     * @param data
     *            the raw data array
     * @param dataDist
     *            the data distirbution.
     * @param minmax
     *            the data range
     *
     * @return non-negative if successful; otherwise, returns negative
     */
    public static int findDataDist(Object data, int[] dataDist, double[] minmax)
    {
        int retval  = 0;
        double delt = 1;

        if ((data == null) || (minmax == null) || dataDist == null)
            return -1;

        int n = Array.getLength(data);

        if (minmax[1] != minmax[0])
            delt = (dataDist.length - 1) / (minmax[1] - minmax[0]);

        for (int i = 0; i < dataDist.length; i++)
            dataDist[i] = 0;

        int idx;
        double val;
        for (int i = 0; i < n; i++) {
            val = ((Number)Array.get(data, i)).doubleValue();
            if (val >= minmax[0] && val <= minmax[1]) {
                idx = (int)((val - minmax[0]) * delt);
                dataDist[idx]++;
            } // don't count invalid values
        }

        return retval;
    }

    /**
     * Computes mean and standard deviation of a data array
     *
     * @param data
     *            the raw data array
     * @param avgstd
     *            the statistics: avgstd[0]=mean and avgstd[1]=stdev.
     * @param fillValue
     *            the missing value or fill value. Exclude this value when
     *            compute statistics
     *
     * @return non-negative if successful; otherwise, returns negative
     */
    public static int computeStatistics(Object data, double[] avgstd, Object fillValue)
    {
        int retval  = 1;
        double sum  = 0;
        double avg  = 0.0;
        double var  = 0.0;
        double diff = 0.0;
        double fill = 0.0;

        if ((data == null) || (avgstd == null) || (Array.getLength(data) <= 0) ||
            (Array.getLength(avgstd) < 2)) {
            return -1;
        }

        int n                = Array.getLength(data);
        boolean hasFillValue = (fillValue != null && fillValue.getClass().isArray());

        String cname = data.getClass().getName();
        char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
        log.trace("computeStatistics() cname={} : dname={}", cname, dname);

        int npoints = 0;
        switch (dname) {
        case 'B':
            byte[] b = (byte[])data;
            if (hasFillValue)
                fill = ((byte[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if (hasFillValue && b[i] == fill)
                    continue;
                sum += b[i];
                npoints++;
            }
            if (npoints > 0) {
                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && b[i] == fill)
                        continue;
                    diff = b[i] - avg;
                    var += diff * diff;
                }
            }
            break;
        case 'S':
            short[] s = (short[])data;
            if (hasFillValue)
                fill = ((short[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if (hasFillValue && s[i] == fill)
                    continue;
                sum += s[i];
                npoints++;
            }
            if (npoints > 0) {
                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && s[i] == fill)
                        continue;
                    diff = s[i] - avg;
                    var += diff * diff;
                }
            }
            break;
        case 'I':
            int[] ia = (int[])data;
            if (hasFillValue)
                fill = ((int[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if (hasFillValue && ia[i] == fill)
                    continue;
                sum += ia[i];
                npoints++;
            }
            if (npoints > 0) {
                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && ia[i] == fill)
                        continue;
                    diff = ia[i] - avg;
                    var += diff * diff;
                }
            }
            break;
        case 'J':
            long[] l = (long[])data;
            if (hasFillValue)
                fill = ((long[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if (hasFillValue && l[i] == fill)
                    continue;
                sum += l[i];
                npoints++;
            }
            if (npoints > 0) {
                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && l[i] == fill)
                        continue;
                    diff = l[i] - avg;
                    var += diff * diff;
                }
            }
            break;
        case 'F':
            float[] f = (float[])data;
            if (hasFillValue)
                fill = ((float[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if (hasFillValue && f[i] == fill)
                    continue;
                sum += f[i];
                npoints++;
            }
            if (npoints > 0) {
                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && f[i] == fill)
                        continue;
                    diff = f[i] - avg;
                    var += diff * diff;
                }
            }
            break;
        case 'D':
            double[] d = (double[])data;
            if (hasFillValue)
                fill = ((double[])fillValue)[0];
            for (int i = 0; i < n; i++) {
                if (hasFillValue && d[i] == fill)
                    continue;
                sum += d[i];
                npoints++;
            }
            if (npoints > 0) {
                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && d[i] == fill)
                        continue;
                    diff = d[i] - avg;
                    var += diff * diff;
                }
            }
            break;
        default:
            retval = -1;
            break;
        } // (dname)

        if (npoints <= 1) {
            if (npoints < 1)
                avgstd[0] = fill;
            avgstd[1] = 0;
        }
        else {
            avgstd[0] = avg;
            avgstd[1] = Math.sqrt(var / (npoints - 1));
        }

        return retval;
    }

    /**
     * Save the data as binary
     *
     * @param out
     *            the output stream
     * @param data
     *            the raw data array
     * @param order
     *            the order of bytes
     *
     * @throws Exception if a failure occurred
     */
    public static void saveAsBinary(DataOutputStream out, Object data, ByteOrder order) throws Exception
    {
        String cname  = data.getClass().getName();
        char dname    = cname.charAt(cname.lastIndexOf('[') + 1);
        ByteBuffer bb = null;

        int size = Array.getLength(data);

        if (dname == 'B') {
            byte[] bdata = (byte[])data;

            bb = ByteBuffer.allocate(BYTE_BUFFER_SIZE);
            bb.order(order);

            int remainingSize   = size - BYTE_BUFFER_SIZE;
            int allocValue      = 0;
            int iterationNumber = 0;
            do {
                if (remainingSize <= 0)
                    allocValue = remainingSize + BYTE_BUFFER_SIZE;
                else
                    allocValue = BYTE_BUFFER_SIZE;
                bb.clear();
                bb.put(bdata, (iterationNumber * BYTE_BUFFER_SIZE), allocValue);
                out.write(bb.array(), 0, allocValue);
                remainingSize = remainingSize - BYTE_BUFFER_SIZE;
                iterationNumber++;
            } while (remainingSize > -BYTE_BUFFER_SIZE);

            out.flush();
            out.close();
        }
        else if (dname == 'S') {
            short[] sdata = (short[])data;
            bb            = ByteBuffer.allocate(SHORT_BUFFER_SIZE * 2);
            bb.order(order);

            ShortBuffer sb      = bb.asShortBuffer();
            int remainingSize   = size - SHORT_BUFFER_SIZE;
            int allocValue      = 0;
            int iterationNumber = 0;
            do {
                if (remainingSize <= 0)
                    allocValue = remainingSize + SHORT_BUFFER_SIZE;
                else
                    allocValue = SHORT_BUFFER_SIZE;
                bb.clear();
                sb.clear();
                sb.put(sdata, (iterationNumber * SHORT_BUFFER_SIZE), allocValue);
                out.write(bb.array(), 0, allocValue * 2);
                remainingSize = remainingSize - SHORT_BUFFER_SIZE;
                iterationNumber++;
            } while (remainingSize > -SHORT_BUFFER_SIZE);

            out.flush();
            out.close();
        }
        else if (dname == 'I') {
            int[] idata = (int[])data;
            bb          = ByteBuffer.allocate(INT_BUFFER_SIZE * 4);
            bb.order(order);

            IntBuffer ib        = bb.asIntBuffer();
            int remainingSize   = size - INT_BUFFER_SIZE;
            int allocValue      = 0;
            int iterationNumber = 0;
            do {
                if (remainingSize <= 0)
                    allocValue = remainingSize + INT_BUFFER_SIZE;
                else
                    allocValue = INT_BUFFER_SIZE;
                bb.clear();
                ib.clear();
                ib.put(idata, (iterationNumber * INT_BUFFER_SIZE), allocValue);
                out.write(bb.array(), 0, allocValue * 4);
                remainingSize = remainingSize - INT_BUFFER_SIZE;
                iterationNumber++;
            } while (remainingSize > -INT_BUFFER_SIZE);

            out.flush();
            out.close();
        }
        else if (dname == 'J') {
            long[] ldata = (long[])data;

            bb = ByteBuffer.allocate(LONG_BUFFER_SIZE * 8);
            bb.order(order);

            LongBuffer lb       = bb.asLongBuffer();
            int remainingSize   = size - LONG_BUFFER_SIZE;
            int allocValue      = 0;
            int iterationNumber = 0;
            do {
                if (remainingSize <= 0)
                    allocValue = remainingSize + LONG_BUFFER_SIZE;
                else
                    allocValue = LONG_BUFFER_SIZE;
                bb.clear();
                lb.clear();
                lb.put(ldata, (iterationNumber * LONG_BUFFER_SIZE), allocValue);
                out.write(bb.array(), 0, allocValue * 8);
                remainingSize = remainingSize - LONG_BUFFER_SIZE;
                iterationNumber++;
            } while (remainingSize > -LONG_BUFFER_SIZE);

            out.flush();
            out.close();
        }
        else if (dname == 'F') {
            float[] fdata = (float[])data;

            bb = ByteBuffer.allocate(FLOAT_BUFFER_SIZE * 4);
            bb.order(order);

            FloatBuffer fb      = bb.asFloatBuffer();
            int remainingSize   = size - FLOAT_BUFFER_SIZE;
            int allocValue      = 0;
            int iterationNumber = 0;
            do {
                if (remainingSize <= 0)
                    allocValue = remainingSize + FLOAT_BUFFER_SIZE;
                else
                    allocValue = FLOAT_BUFFER_SIZE;
                bb.clear();
                fb.clear();
                fb.put(fdata, (iterationNumber * FLOAT_BUFFER_SIZE), allocValue);
                out.write(bb.array(), 0, allocValue * 4);
                remainingSize = remainingSize - FLOAT_BUFFER_SIZE;
                iterationNumber++;
            } while (remainingSize > -FLOAT_BUFFER_SIZE);

            out.flush();
            out.close();
        }
        else if (dname == 'D') {
            double[] ddata = (double[])data;

            bb = ByteBuffer.allocate(DOUBLE_BUFFER_SIZE * 8);
            bb.order(order);

            DoubleBuffer db     = bb.asDoubleBuffer();
            int remainingSize   = size - DOUBLE_BUFFER_SIZE;
            int allocValue      = 0;
            int iterationNumber = 0;
            do {
                if (remainingSize <= 0)
                    allocValue = remainingSize + DOUBLE_BUFFER_SIZE;
                else
                    allocValue = DOUBLE_BUFFER_SIZE;
                bb.clear();
                db.clear();
                db.put(ddata, (iterationNumber * DOUBLE_BUFFER_SIZE), allocValue);
                out.write(bb.array(), 0, allocValue * 8);
                remainingSize = remainingSize - DOUBLE_BUFFER_SIZE;
                iterationNumber++;
            } while (remainingSize > -DOUBLE_BUFFER_SIZE);

            out.flush();
            out.close();
        }
    }

    /**
     * Reads data from a binary file into a buffer.
     *
     * @param dataOut
     *            the output stream
     * @param fileName
     *            the file to read binary data from
     * @param order
     *            the new byte order, either BIG_ENDIAN or LITTLE_ENDIAN
     *
     * @return true if successful; otherwise, false.
     */
    public static boolean getBinaryDataFromFile(Object dataOut, String fileName, ByteOrder order)
    {
        if (dataOut == null)
            return false;

        String fname           = fileName;
        BufferedInputStream in = null;
        ByteBuffer byteBuffer  = null;
        boolean valChanged     = false;

        try (FileInputStream inputFile = new FileInputStream(fname)) {
            long fileSize = inputFile.getChannel().size();
            in            = new BufferedInputStream(inputFile);

            int datasetSize = Array.getLength(dataOut);
            String cname    = dataOut.getClass().getName();
            char dname      = cname.charAt(cname.lastIndexOf('[') + 1);

            if (dname == 'B') {
                long datasetByteSize = datasetSize;
                byteBuffer           = ByteBuffer.allocate(BYTE_BUFFER_SIZE);
                byteBuffer.order(order);

                int bufferSize = (int)Math.min(fileSize, datasetByteSize);

                int remainingSize   = bufferSize - (BYTE_BUFFER_SIZE);
                int allocValue      = 0;
                int iterationNumber = 0;
                byte[] byteArray    = new byte[BYTE_BUFFER_SIZE];
                do {
                    if (remainingSize <= 0)
                        allocValue = remainingSize + (BYTE_BUFFER_SIZE);
                    else
                        allocValue = (BYTE_BUFFER_SIZE);

                    in.read(byteBuffer.array(), 0, allocValue);

                    byteBuffer.get(byteArray, 0, allocValue);
                    System.arraycopy(byteArray, 0, dataOut, (iterationNumber * BYTE_BUFFER_SIZE), allocValue);
                    byteBuffer.clear();
                    remainingSize = remainingSize - (BYTE_BUFFER_SIZE);
                    iterationNumber++;
                } while (remainingSize > -(BYTE_BUFFER_SIZE));

                valChanged = true;
            }
            else if (dname == 'S') {
                long datasetShortSize = (long)datasetSize * 2;
                byteBuffer            = ByteBuffer.allocate(SHORT_BUFFER_SIZE * 2);
                byteBuffer.order(order);

                int bufferSize      = (int)Math.min(fileSize, datasetShortSize);
                int remainingSize   = bufferSize - (SHORT_BUFFER_SIZE * 2);
                int allocValue      = 0;
                int iterationNumber = 0;
                ShortBuffer sb      = byteBuffer.asShortBuffer();
                short[] shortArray  = new short[SHORT_BUFFER_SIZE];

                do {
                    if (remainingSize <= 0)
                        allocValue = remainingSize + (SHORT_BUFFER_SIZE * 2);
                    else
                        allocValue = (SHORT_BUFFER_SIZE * 2);
                    in.read(byteBuffer.array(), 0, allocValue);
                    sb.get(shortArray, 0, allocValue / 2);
                    System.arraycopy(shortArray, 0, dataOut, (iterationNumber * SHORT_BUFFER_SIZE),
                                     allocValue / 2);
                    byteBuffer.clear();
                    sb.clear();
                    remainingSize = remainingSize - (SHORT_BUFFER_SIZE * 2);
                    iterationNumber++;
                } while (remainingSize > -(SHORT_BUFFER_SIZE * 2));

                valChanged = true;
            }
            else if (dname == 'I') {
                long datasetIntSize = (long)datasetSize * 4;
                byteBuffer          = ByteBuffer.allocate(INT_BUFFER_SIZE * 4);
                byteBuffer.order(order);

                int bufferSize      = (int)Math.min(fileSize, datasetIntSize);
                int remainingSize   = bufferSize - (INT_BUFFER_SIZE * 4);
                int allocValue      = 0;
                int iterationNumber = 0;
                int[] intArray      = new int[INT_BUFFER_SIZE];
                byte[] tmpBuf       = byteBuffer.array();
                IntBuffer ib        = byteBuffer.asIntBuffer();

                do {
                    if (remainingSize <= 0)
                        allocValue = remainingSize + (INT_BUFFER_SIZE * 4);
                    else
                        allocValue = (INT_BUFFER_SIZE * 4);
                    in.read(tmpBuf, 0, allocValue);
                    ib.get(intArray, 0, allocValue / 4);
                    System.arraycopy(intArray, 0, dataOut, (iterationNumber * INT_BUFFER_SIZE),
                                     allocValue / 4);
                    byteBuffer.clear();
                    ib.clear();
                    remainingSize = remainingSize - (INT_BUFFER_SIZE * 4);
                    iterationNumber++;
                } while (remainingSize > -(INT_BUFFER_SIZE * 4));

                valChanged = true;
            }
            else if (dname == 'J') {
                long datasetLongSize = (long)datasetSize * 8;
                byteBuffer           = ByteBuffer.allocate(LONG_BUFFER_SIZE * 8);
                byteBuffer.order(order);

                int bufferSize      = (int)Math.min(fileSize, datasetLongSize);
                int remainingSize   = bufferSize - (LONG_BUFFER_SIZE * 8);
                int allocValue      = 0;
                int iterationNumber = 0;
                long[] longArray    = new long[LONG_BUFFER_SIZE];
                LongBuffer lb       = byteBuffer.asLongBuffer();

                do {
                    if (remainingSize <= 0)
                        allocValue = remainingSize + (LONG_BUFFER_SIZE * 8);
                    else
                        allocValue = (LONG_BUFFER_SIZE * 8);

                    in.read(byteBuffer.array(), 0, allocValue);
                    lb.get(longArray, 0, allocValue / 8);
                    System.arraycopy(longArray, 0, dataOut, (iterationNumber * LONG_BUFFER_SIZE),
                                     allocValue / 8);
                    byteBuffer.clear();
                    lb.clear();
                    remainingSize = remainingSize - (LONG_BUFFER_SIZE * 8);
                    iterationNumber++;
                } while (remainingSize > -(LONG_BUFFER_SIZE * 8));

                valChanged = true;
            }
            else if (dname == 'F') {
                long datasetFloatSize = (long)datasetSize * 4;
                byteBuffer            = ByteBuffer.allocate(FLOAT_BUFFER_SIZE * 4);
                byteBuffer.order(order);

                int bufferSize      = (int)Math.min(fileSize, datasetFloatSize);
                int remainingSize   = bufferSize - (FLOAT_BUFFER_SIZE * 4);
                int allocValue      = 0;
                int iterationNumber = 0;
                FloatBuffer fb      = byteBuffer.asFloatBuffer();
                float[] floatArray  = new float[FLOAT_BUFFER_SIZE];
                do {
                    if (remainingSize <= 0)
                        allocValue = remainingSize + (FLOAT_BUFFER_SIZE * 4);
                    else
                        allocValue = (FLOAT_BUFFER_SIZE * 4);

                    in.read(byteBuffer.array(), 0, allocValue);
                    fb.get(floatArray, 0, allocValue / 4);
                    System.arraycopy(floatArray, 0, dataOut, (iterationNumber * FLOAT_BUFFER_SIZE),
                                     allocValue / 4);
                    byteBuffer.clear();
                    fb.clear();
                    remainingSize = remainingSize - (FLOAT_BUFFER_SIZE * 4);
                    iterationNumber++;
                } while (remainingSize > -(FLOAT_BUFFER_SIZE * 4));

                valChanged = true;
            }
            else if (dname == 'D') {
                long datasetDoubleSize = (long)datasetSize * 8;
                byteBuffer             = ByteBuffer.allocate(DOUBLE_BUFFER_SIZE * 8);
                byteBuffer.order(order);

                int bufferSize       = (int)Math.min(fileSize, datasetDoubleSize);
                int remainingSize    = bufferSize - (DOUBLE_BUFFER_SIZE * 8);
                int allocValue       = 0;
                int iterationNumber  = 0;
                DoubleBuffer db      = byteBuffer.asDoubleBuffer();
                double[] doubleArray = new double[DOUBLE_BUFFER_SIZE];

                do {
                    if (remainingSize <= 0)
                        allocValue = remainingSize + (DOUBLE_BUFFER_SIZE * 8);
                    else
                        allocValue = (DOUBLE_BUFFER_SIZE * 8);

                    in.read(byteBuffer.array(), 0, allocValue);
                    db.get(doubleArray, 0, allocValue / 8);
                    System.arraycopy(doubleArray, 0, dataOut, (iterationNumber * DOUBLE_BUFFER_SIZE),
                                     allocValue / 8);
                    byteBuffer.clear();
                    db.clear();
                    remainingSize = remainingSize - (DOUBLE_BUFFER_SIZE * 8);
                    iterationNumber++;
                } while (remainingSize > -(DOUBLE_BUFFER_SIZE * 8));

                valChanged = true;
            }
        }
        catch (Exception es) {
            es.printStackTrace();
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ex) {
                // Empty on purpose
            }
        }

        return valChanged;
    }

    /**
     * Returns a string representation of the long argument as an unsigned
     * integer in base 2. This is different from Long.toBinaryString(long i).
     * This function add padding (0's) to the string based on the nbytes. For
     * example, if v=15, nbytes=1, the string will be "00001111".
     *
     * @param v
     *            the long value
     * @param nbytes
     *            number of bytes in the integer
     *
     * @return the string representation of the unsigned long value represented
     *         by the argument in binary (base 2).
     */
    public static final String toBinaryString(long v, int nbytes)
    {
        if (nbytes <= 0)
            return null;

        int nhex    = nbytes * 2;
        short[] hex = new short[nhex];

        for (int i = 0; i < nhex; i++)
            hex[i] = (short)(0x0F & (v >> (i * 4)));

        StringBuilder sb = new StringBuilder();
        boolean isEven   = true;
        for (int i = nhex - 1; i >= 0; i--) {
            if (isEven && i < nhex - 1)
                sb.append(" ");
            isEven = !isEven; // toggle

            switch (hex[i]) {
            case 0:
                sb.append("0000");
                break;
            case 1:
                sb.append("0001");
                break;
            case 2:
                sb.append("0010");
                break;
            case 3:
                sb.append("0011");
                break;
            case 4:
                sb.append("0100");
                break;
            case 5:
                sb.append("0101");
                break;
            case 6:
                sb.append("0110");
                break;
            case 7:
                sb.append("0111");
                break;
            case 8:
                sb.append("1000");
                break;
            case 9:
                sb.append("1001");
                break;
            case 10:
                sb.append("1010");
                break;
            case 11:
                sb.append("1011");
                break;
            case 12:
                sb.append("1100");
                break;
            case 13:
                sb.append("1101");
                break;
            case 14:
                sb.append("1110");
                break;
            case 15:
                sb.append("1111");
                break;
            default:
                break;
            }
        }

        return sb.toString();
    }

    /**
     * Returns a string representation of the BigDecimal argument as an unsigned
     * integer in base 2. This is different from BigDecimal.toBinaryString(long i).
     * This function add padding (0's) to the string based on the nbytes. For
     * example, if v=15, nbytes=1, the string will be "00001111".
     *
     * @param v
     *            the BigDecimal value
     * @param nbytes
     *            number of bytes in the integer
     *
     * @return the string representation of the BigDecimal value represented
     *         by the argument in binary (base 2).
     */
    public static final String toBinaryString(BigDecimal v, int nbytes)
    {
        StringBuilder sb = new StringBuilder();
        /*
         * String val = String.format("%" + (8 * nbytes) + "s", v.toString(2)).replace(" ",
         * "0").toUpperCase(); // Insert spacing for (int i = 0; i < nbytes; i++) { sb.append(val.substring(i
         * * nbytes, nbytes * (i + 1))); if (i < nbytes - 1) sb.append(" "); }
         */
        return sb.toString();
    }

    /**
     * Returns a string representation of the BigInteger argument as an unsigned
     * integer in base 2. This is different from BigInteger.toBinaryString(long i).
     * This function add padding (0's) to the string based on the nbytes. For
     * example, if v=15, nbytes=1, the string will be "00001111".
     *
     * @param v
     *            the BigInteger value
     * @param nbytes
     *            number of bytes in the integer
     *
     * @return the string representation of the BigInteger value represented
     *         by the argument in binary (base 2).
     */
    public static final String toBinaryString(BigInteger v, int nbytes)
    {
        StringBuilder sb = new StringBuilder();
        String val = String.format("%" + (8 * nbytes) + "s", v.toString(2)).replace(" ", "0").toUpperCase();

        // Insert spacing
        for (int i = 0; i < nbytes; i++) {
            sb.append(val.substring(i * nbytes, nbytes * (i + 1)));
            if (i < nbytes - 1)
                sb.append(" ");
        }

        return sb.toString();
    }

    /**
     * Returns a string representation of the long argument as an unsigned integer in base 16. This
     * is different from Long.toHexString(long i). This function add padding (0's) to the string
     * based on the nbytes. For example, if v=42543, nbytes=4, the string will be "0000A62F".
     *
     * @param v
     *            the long value
     * @param nbytes
     *            number of bytes in the integer
     * @return the string representation of the unsigned long value represented by the argument in
     *         hexadecimal (base 16).
     */
    public static final String toHexString(long v, int nbytes)
    {
        char[] HEXCHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        if (nbytes <= 0)
            return null;

        int nhex    = nbytes * 2;
        short[] hex = new short[nhex];

        for (int i = 0; i < nhex; i++)
            hex[i] = (short)(0x0F & (v >> (i * 4)));

        StringBuilder sb = new StringBuilder();
        for (int i = nhex - 1; i >= 0; i--)
            sb.append(HEXCHARS[hex[i]]);

        return sb.toString();
    }

    /**
     * Returns a string representation of the BigInteger argument as an unsigned integer in base 16.
     * This is different from BigInteger.toString(16). This function adds padding (0's) to the string
     * based on the nbytes. For example, if v=42543, nbytes=4, the string will be "0000A62F".
     *
     * @param v
     *            the BigInteger value
     * @param nbytes
     *            number of bytes in the integer
     * @return the string representation of the BigInteger value represented by the argument in
     *         hexadecimal (base 16).
     */
    public static final String toHexString(BigInteger v, int nbytes)
    {
        return String.format("%" + (2 * nbytes) + "s", v.toString(16)).replace(" ", "0").toUpperCase();
    }

    /**
     * Returns a string representation of the BigDecimal argument as an unsigned integer in base 16.
     * This is different from BigDecimal.toString(16). This function adds padding (0's) to the string
     * based on the nbytes. For example, if v=42543, nbytes=4, the string will be "0000A62F".
     *
     * @param v
     *            the BigDecimal value
     * @param nbytes
     *            number of bytes in the integer
     * @return the string representation of the BigDecimal value represented by the argument in
     *         hexadecimal (base 16).
     */
    public static final String toHexString(BigDecimal v, int nbytes)
    {
        return null; // String.format("%" + (2 * nbytes) + "s", v.toString(16)).replace(" ",
                     // "0").toUpperCase();
    }

    /**
     * Apply bitmask to a data array.
     *
     * @param theData
     *            the data array which the bitmask is applied to.
     * @param theMask
     *            the bitmask to be applied to the data array.
     * @param op
     *            the bitmask op to be applied
     *
     * @return true if bitmask is applied successfully; otherwise, false.
     */
    public static final boolean applyBitmask(Object theData, BitSet theMask, ViewProperties.BITMASK_OP op)
    {
        if (theData == null || !(theData instanceof Array) ||
            ((theData instanceof Array) && (Array.getLength(theData) <= 0)) || theMask == null)
            return false;

        char nt      = '0';
        String cName = theData.getClass().getName();
        int cIndex   = cName.lastIndexOf('[');
        if (cIndex >= 0)
            nt = cName.charAt(cIndex + 1);

        // only deal with 8/16/32/64 bit datasets
        if (!(nt == 'B' || nt == 'S' || nt == 'I' || nt == 'J'))
            return false;

        long bmask       = 0;
        long theValue    = 0;
        long packedValue = 0;

        int nbits = theMask.length();
        int len   = Array.getLength(theData);

        for (int i = 0; i < nbits; i++)
            if (theMask.get(i))
                bmask += 1 << i;

        for (int i = 0; i < len; i++) {
            if (nt == 'B')
                theValue = ((byte[])theData)[i] & bmask;
            else if (nt == 'S')
                theValue = ((short[])theData)[i] & bmask;
            else if (nt == 'I')
                theValue = ((int[])theData)[i] & bmask;
            else if (nt == 'J')
                theValue = ((long[])theData)[i] & bmask;

            // apply bitmask only
            if (op == BITMASK_OP.AND)
                packedValue = theValue;
            else {
                // extract bits
                packedValue     = 0;
                int bitPosition = 0;

                for (int j = 0; j < nbits; j++) {
                    if (theMask.get(j)) {
                        long bitValue = (theValue & 1);
                        packedValue += (bitValue << bitPosition);
                        bitPosition++;
                    }
                    // move to the next bit
                    theValue = theValue >> 1;
                }
            }

            if (nt == 'B')
                ((byte[])theData)[i] = (byte)packedValue;
            else if (nt == 'S')
                ((short[])theData)[i] = (short)packedValue;
            else if (nt == 'I')
                ((int[])theData)[i] = (int)packedValue;
            else if (nt == 'J')
                ((long[])theData)[i] = packedValue;
        } // (int i = 0; i < len; i++)

        return true;
    } /* public static final boolean applyBitmask() */

    /**
     * Read HDF5 user block data into byte array.
     *
     * @param filename the HDF5 file from which to get the user block
     *
     * @return a byte array of user block, or null if there is user data.
     */
    public static byte[] getHDF5UserBlock(String filename)
    {
        byte[] userBlock = null;

        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            byte[] header = new byte[8];
            long fileSize = raf.length();

            // The super block is located by searching for the HDF5 file signature
            // at byte offset 0, byte offset 512 and at successive locations in the
            // file, each a multiple of two of the previous location, i.e. 0, 512,
            // 1024, 2048, etc
            long offset  = 0;
            boolean ish5 = false;
            while (offset < fileSize) {
                raf.seek(offset);
                raf.read(header);

                if ((header[0] == -119) && (header[1] == 72) && (header[2] == 68) && (header[3] == 70) &&
                    (header[4] == 13) && (header[5] == 10) && (header[6] == 26) && (header[7] == 10)) {
                    ish5 = true;
                    break; // find the end of user block
                }
                else {
                    ish5 = false;
                    if (offset == 0)
                        offset = 512;
                    else
                        offset *= 2;
                }
            }

            if (!ish5 || (offset == 0))
                return null;

            int blockSize = (int)offset;
            userBlock     = new byte[blockSize];
            raf.seek(0);
            raf.read(userBlock, 0, blockSize);
        }
        catch (Exception ex) {
            userBlock = null;
        }

        return userBlock;
    }

    /**
     * Write HDF5 user block data into byte array.
     *
     * @param fin the input filename
     * @param fout the output filename
     * @param buf  the data to write into the user block
     *
     * @return a byte array of user block, or null if there is user data.
     */
    public static boolean setHDF5UserBlock(String fin, String fout, byte[] buf)
    {
        boolean ish5 = false;

        if ((buf == null) || (buf.length <= 0))
            return false;

        File tmpFile = new File(fin);
        if (!tmpFile.exists())
            return false;

        long offset = 0;
        // find the end of user block for the input file
        try (RandomAccessFile raf = new RandomAccessFile(fin, "r")) {
            byte[] header = new byte[8];
            long fileSize = raf.length();

            // The super block is located by searching for the HDF5 file signature
            // at byte offset 0, byte offset 512 and at successive locations in the
            // file, each a multiple of two of the previous location, i.e. 0, 512,
            // 1024, 2048, etc
            while (offset < fileSize) {
                raf.seek(offset);
                raf.read(header);

                if ((header[0] == -119) && (header[1] == 72) && (header[2] == 68) && (header[3] == 70) &&
                    (header[4] == 13) && (header[5] == 10) && (header[6] == 26) && (header[7] == 10)) {
                    ish5 = true;
                    break;
                }
                else {
                    ish5 = false;
                    if (offset == 0)
                        offset = 512;
                    else
                        offset *= 2;
                }
            }
        }
        catch (Exception ex) {
            return false;
        }

        if (!ish5)
            return false;

        int length = 0;
        int bsize  = 1024;
        byte[] buffer;

        try (BufferedInputStream bi = new BufferedInputStream(new FileInputStream(fin))) {
            try (BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(fout))) {
                // skip the header of original file
                try {
                    long count = bi.skip(offset);
                    if (count != offset)
                        log.debug("file skip actual:{} req:{}", count, offset);
                }
                catch (Exception ex) {
                    // Empty on purpose
                }

                // write the header into the new file
                try {
                    bo.write(buf, 0, buf.length);
                }
                catch (Exception ex) {
                    // Empty on purpose
                }

                // The super block space is allocated by offset 0, 512, 1024, 2048, etc
                offset = 512;
                while (offset < buf.length)
                    offset *= 2;

                int padSize = (int)(offset - buf.length);
                if (padSize > 0) {
                    byte[] padBuf = new byte[padSize];
                    try {
                        bo.write(padBuf, 0, padSize);
                    }
                    catch (Exception ex) {
                        // Empty on purpose
                    }
                }

                // copy the hdf5 file content from input file to the output file
                buffer = new byte[bsize];
                try {
                    length = bi.read(buffer, 0, bsize);
                }
                catch (Exception ex) {
                    length = 0;
                }
                while (length > 0) {
                    try {
                        bo.write(buffer, 0, length);
                        length = bi.read(buffer, 0, bsize);
                    }
                    catch (Exception ex) {
                        length = 0;
                    }
                }

                try {
                    bo.flush();
                }
                catch (Exception ex) {
                    // Empty on purpose
                }
            }
            catch (Exception ex) {
                return false;
            }
        }
        catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * look at the first 4 bytes of the file to see if it is an HDF4 file.
     * byte[0]=14, byte[1]=3, byte[2]=19, byte[3]=1 or if it is a netCDF file
     * byte[0]=67, byte[1]=68, byte[2]=70, byte[3]=1
     *
     * @param filename the file to test if HDF4
     *
     * @return true if the file is of type HDF4
     */
    public static boolean isHDF4(String filename)
    {
        boolean ish4 = false;

        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            byte[] header = new byte[4];
            raf.read(header);

            if ((header[0] == 14) && (header[1] == 3) && (header[2] == 19) && (header[3] == 1))
                ish4 = true;
            else
                ish4 = false;
        }
        catch (Exception ex) {
            return false;
        }

        return ish4;
    }

    /**
     * look at the first 8 bytes of the file to see if it is an HDF5 file.
     * byte[0]=-199 which is 137 in unsigned byte, byte[1]=72, byte[2]=68,
     * byte[3]=70, byte[4]=13, byte[5]=10, byte[6]=26, byte[7]=10
     *
     * @param filename the file to test if HDF5
     *
     * @return true if the file is of type HDF5
     */
    public static boolean isHDF5(String filename)
    {
        boolean ish5 = false;

        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            byte[] header = new byte[8];
            long fileSize = raf.length();

            // The super block is located by searching for the HDF5 file signature
            // at byte offset 0, byte offset 512 and at successive locations in the
            // file, each a multiple of two of the previous location, i.e. 0, 512,
            // 1024, 2048, etc
            long offset = 0;
            while (!ish5 && (offset < fileSize)) {
                raf.seek(offset);
                raf.read(header);

                if ((header[0] == -119) && (header[1] == 72) && (header[2] == 68) && (header[3] == 70) &&
                    (header[4] == 13) && (header[5] == 10) && (header[6] == 26) && (header[7] == 10)) {
                    ish5 = true;
                }
                else {
                    ish5 = false;
                    if (offset == 0)
                        offset = 512;
                    else
                        offset *= 2;
                }
            }
        }
        catch (Exception ex) {
            return false;
        }

        return ish5;
    }

    /**
     * look at the first 4 bytes of the file to see if it is a netCDF file
     * byte[0]=67, byte[1]=68, byte[2]=70, byte[3]=1 or
     *
     * @param filename the file to test if netcdf
     *
     * @return true if the file is of type netcdf
     */
    public static boolean isNetcdf(String filename)
    {
        boolean isnc = false;

        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {

            byte[] header = new byte[4];
            raf.read(header);
            // netCDF
            if ((header[0] == 67) && (header[1] == 68) && (header[2] == 70) && (header[3] == 1))
                isnc = true;
            else
                isnc = false;
        }
        catch (Exception ex) {
            return false;
        }

        return isnc;
    }

    /**
     * Launch default browser for a given URL.
     *
     * @param url
     *            the URL to open.
     *
     * @throws Exception if a failure occurred
     */
    public static final void launchBrowser(String url) throws Exception
    {
        String os       = System.getProperty("os.name");
        Runtime runtime = Runtime.getRuntime();

        // Block for Windows Platform
        if (os.startsWith("Windows")) {
            String cmd = "rundll32 url.dll,FileProtocolHandler " + url;

            if (new File(url).exists())
                cmd = "cmd /c start \"\" \"" + url + "\"";
            runtime.exec(cmd);
        }
        // Block for Mac OS
        else if (os.startsWith("Mac OS")) {
            Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
            Method openURL   = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class});

            // local file
            if (new File(url).exists())
                url = "file://" + url;

            openURL.invoke(null, new Object[] {url});
        }
        // Block for UNIX Platform
        else {
            String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape"};
            String browser    = null;
            for (int count = 0; count < browsers.length && browser == null; count++)
                if (runtime.exec(new String[] {"which", browsers[count]}).waitFor() == 0)
                    browser = browsers[count];
            if (browser == null)
                throw new Exception("Could not find web browser");
            else
                runtime.exec(new String[] {browser, url});
        }
    }

    /**
     * Create a new HDF file with default file creation properties
     *
     * @param filename
     *          the file to create
     * @param dir
     *          the directory for file
     * @param type
     *          the type of the file
     * @param openFiles
     *          the list of already opened files
     *
     * @return the FileFormat instance of the newly created file
     *
     * @throws Exception if a failure occurred
     */
    public static FileFormat createNewFile(String filename, String dir, String type,
                                           List<FileFormat> openFiles) throws Exception
    {
        log.trace("createNewFile: {} start", filename);
        File f = new File(filename);

        String fname = f.getAbsolutePath();
        if (fname == null)
            return null;

        fname = fname.trim();
        if ((fname == null) || (fname.length() == 0))
            throw new Exception("Invalid file name.");

        String extensions   = FileFormat.getFileExtensions();
        boolean noExtension = true;
        if ((extensions != null) && (extensions.length() > 0)) {
            java.util.StringTokenizer currentExt = new java.util.StringTokenizer(extensions, ",");
            String extension                     = "";
            String tmpFilename                   = fname.toLowerCase();
            while (currentExt.hasMoreTokens() && noExtension) {
                extension   = currentExt.nextToken().trim().toLowerCase();
                noExtension = !tmpFilename.endsWith("." + extension);
            }
        }

        if (noExtension) {
            if (type.equals(FileFormat.FILE_TYPE_HDF4)) {
                fname += ".hdf";
                f = new File(fname);
            }
            else if (type.equals(FileFormat.FILE_TYPE_HDF5)) {
                fname += ".h5";
                f = new File(fname);
            }
        }

        if (f.exists() && f.isDirectory())
            throw new Exception("File is a directory.");
        log.trace("createNewFile: {} not a directory", filename);

        File pfile = f.getParentFile();
        if (pfile == null) {
            fname = dir + File.separator + fname;
            f     = new File(fname);
        }
        else if (!pfile.exists()) {
            throw new Exception("File path does not exist at\n" + pfile.getPath());
        }

        // check if the file is in use
        log.trace("createNewFile: {} check if the file is in use", filename);
        if (openFiles != null) {
            FileFormat theFile            = null;
            Iterator<FileFormat> iterator = openFiles.iterator();
            while (iterator.hasNext()) {
                theFile = iterator.next();
                if (theFile.getFilePath().equals(fname))
                    throw new Exception("Unable to create the new file. \nThe file is being used.");
            }
        }

        if (f.exists()) {
            log.trace("createNewFile: {} file exists", filename);

            if (!MessageDialog.openConfirm(display.getShells()[0], "Create New File",
                                           "File exists. Do you want to replace it?"))
                return null;
        }

        try {
            int aFlag = FileFormat.FILE_CREATE_DELETE;
            if (!ViewProperties.getEarlyLib().equalsIgnoreCase("Latest")) {
                aFlag = FileFormat.FILE_CREATE_DELETE | FileFormat.FILE_CREATE_EARLY_LIB;
                FileFormat.getFileFormat(type).setNewLibBounds(ViewProperties.getEarlyLib(),
                                                               ViewProperties.getLateLib());
            }
            log.trace("createNewFile: {} FileFormat create", filename);
            return FileFormat.getFileFormat(type).createFile(fname, aFlag);
        }
        catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    /**
     * Check and find a non-exist file.
     *
     * @param path
     *            -- the path that the new file will be checked.
     * @param ext
     *            -- the extention of the new file.
     *
     * @return -- the new file.
     */
    public static final File checkNewFile(String path, String ext)
    {
        File file = new File(path + "new" + ext);
        int i     = 1;

        while (file.exists()) {
            file = new File(path + "new" + i + ext);
            i++;
        }

        return file;
    }

    /**
     * Check if a given number if NaN or INF.
     *
     * @param val
     *            the number to be checked
     *
     * @return true if the number is Nan or INF; otherwise, false.
     */
    public static final boolean isNaNINF(double val)
    {
        return (Double.isNaN(val) || val == Float.NEGATIVE_INFINITY || val == Float.POSITIVE_INFINITY ||
                val == Double.NEGATIVE_INFINITY || val == Double.POSITIVE_INFINITY);
    }

    /**
     * Since Java does not allow array indices to be larger than int type, check the
     * given value to see if it is within the valid range of a Java int.
     *
     * @param value
     *            The value to check
     *
     * @return false if the value is outside the range of a Java int, true
     *         otherwise.
     */
    public static boolean checkValidJavaArrayIndex(final long value)
    {
        return (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE);
    }

    /**
     * Show an SWT error dialog with the given error message.
     * @param parent
     *           The parent Shell of the MessageDialog
     * @param title
     *           The title to set for the MessageDialog
     * @param errorMsg
     *           The error message to display in the MessageDialog
     */
    public static void showError(Shell parent, String title, String errorMsg)
    {
        String dlgTitlePrefix = "";
        String dlgTitleSuffix = (title == null) ? "" : title;

        if (parent != null) {
            dlgTitlePrefix = parent.getText();
            if (dlgTitlePrefix.length() > 0)
                dlgTitlePrefix += " - ";
        }

        MessageDialog.openError(parent, dlgTitlePrefix + dlgTitleSuffix,
                                (errorMsg == null) ? "UNKNOWN" : errorMsg);
    }

    /**
     * Show an SWT Information dialog with the given message.
     * @param parent
     *           The parent Shell of the MessageDialog
     * @param title
     *           The title to set for the MessageDialog
     * @param infoMsg
     *           The message to display in the MessageDialog
     */
    public static void showInformation(Shell parent, String title, String infoMsg)
    {
        String dlgTitlePrefix = "";
        String dlgTitleSuffix = (title == null) ? "" : title;

        if (parent != null) {
            dlgTitlePrefix = parent.getText();
            if (dlgTitlePrefix.length() > 0)
                dlgTitlePrefix += " - ";
        }

        MessageDialog.openInformation(parent, dlgTitlePrefix + dlgTitleSuffix,
                                      (infoMsg == null) ? "UNKNOWN" : infoMsg);
    }

    /**
     * Show an SWT Confirm dialog with the given message.
     *
     * @param parent
     *            The parent Shell of the MessageDialog
     * @param title
     *            The title to set for the MessageDialog
     * @param confMsg
     *            The message to display in the MessageDialog
     * @return The status of the dialog after closing
     */
    public static boolean showConfirm(Shell parent, String title, String confMsg)
    {
        String dlgTitlePrefix = "";
        String dlgTitleSuffix = (title == null) ? "" : title;

        if (parent != null) {
            dlgTitlePrefix = parent.getText();
            if (dlgTitlePrefix.length() > 0)
                dlgTitlePrefix += " - ";
        }

        return MessageDialog.openConfirm(parent, dlgTitlePrefix + dlgTitleSuffix,
                                         (confMsg == null) ? "UNKNOWN" : confMsg);
    }

    /**
     * Show an SWT Warning dialog with the given message.
     * @param parent
     *           The parent Shell of the MessageDialog
     * @param title
     *           The title to set for the MessageDialog
     * @param warnMsg
     *           The message to display in the MessageDialog
     */
    public static void showWarning(Shell parent, String title, String warnMsg)
    {
        String dlgTitlePrefix = "";
        String dlgTitleSuffix = (title == null) ? "" : title;

        if (parent != null) {
            dlgTitlePrefix = parent.getText();
            if (dlgTitlePrefix.length() > 0)
                dlgTitlePrefix += " - ";
        }

        MessageDialog.openWarning(parent, dlgTitlePrefix + dlgTitleSuffix,
                                  (warnMsg == null) ? "UNKNOWN" : warnMsg);
    }
}
