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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.ScalarDS;
import hdf.view.ViewProperties.BITMASK_OP;

/**
 * The "Tools" class contains various tools for HDF files such as jpeg to HDF
 * converter.
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public final class Tools {
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Tools.class);

    public static final long       MAX_INT8        = 127;
    public static final long       MAX_UINT8       = 255;
    public static final long       MAX_INT16       = 32767;
    public static final long       MAX_UINT16      = 65535;
    public static final long       MAX_INT32       = 2147483647;
    public static final long       MAX_UINT32      = 4294967295L;
    public static final long       MAX_INT64       = 9223372036854775807L;
    public static final BigInteger MAX_UINT64      = new BigInteger("18446744073709551615");

    /** Key for JPEG image file type. */
    public static final String     FILE_TYPE_JPEG  = "JPEG";

    /** Key for TIFF image file type. */
    public static final String     FILE_TYPE_TIFF  = "TIFF";

    /** Key for PNG image file type. */
    public static final String     FILE_TYPE_PNG   = "PNG";

    /** Key for GIF image file type. */
    public static final String     FILE_TYPE_GIF   = "GIF";

    /** Key for BMP image file type. */
    public static final String     FILE_TYPE_BMP   = "BMP";

    /** Key for all image file type. */
    public static final String     FILE_TYPE_IMAGE = "IMG";

    /** Print out debug information
     * @param caller
     *            the caller object.
     * @param msg
     *            the message to be displayed.
     */
    public static final void debug(Object caller, Object msg) {
        if (caller != null) System.out.println("*** " + caller.getClass().getName() + ": " + msg);
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
            throws Exception {
        File imgFile = null;

        if (imgFileName == null) {
            throw new NullPointerException("The source image file is null.");
        }
        else if (!(imgFile = new File(imgFileName)).exists()) {
            throw new NullPointerException("The source image file does not exist.");
        }
        else if (hFileName == null) {
            throw new NullPointerException("The target HDF file is null.");
        }

        if (!fromType.equals(FILE_TYPE_IMAGE)) {
            throw new UnsupportedOperationException("Unsupported image type.");
        }
        else if (!(toType.equals(FileFormat.FILE_TYPE_HDF4) || toType.equals(FileFormat.FILE_TYPE_HDF5))) {
            throw new UnsupportedOperationException("Unsupported destination file type.");
        }

        BufferedImage image = null;
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(imgFileName));
            image = ImageIO.read(in);
            in.close();
        }
        catch (Throwable err) {
            image = null;
        }

        if (image == null) throw new UnsupportedOperationException("Failed to read image: " + imgFileName);

        long h = image.getHeight();
        long w = image.getWidth();
        byte[] data = null;

        try {
            data = new byte[(int)(3 * h * w)];
        }
        catch (OutOfMemoryError err) {
            err.printStackTrace();
            throw new RuntimeException("Out of memory error.");
        }

        int idx = 0;
        int rgb = 0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                rgb = image.getRGB(j, i);
                data[idx++] = (byte) (rgb >> 16);
                data[idx++] = (byte) (rgb >> 8);
                data[idx++] = (byte) rgb;
            }
        }

        long[] dims = null;
        Datatype type = null;
        Group pgroup = null;
        String imgName = imgFile.getName();
        FileFormat newfile = null, thefile = null;
        if (toType.equals(FileFormat.FILE_TYPE_HDF5)) {
            thefile = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
            long[] h5dims = { h, w, 3 }; // RGB pixel interlace
            dims = h5dims;
        }
        else if (toType.equals(FileFormat.FILE_TYPE_HDF4)) {
            thefile = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);
            long[] h4dims = { w, h, 3 }; // RGB pixel interlace
            dims = h4dims;
        }
        else {
            thefile = null;
        }

        if (thefile != null) {
            newfile = thefile.createInstance(hFileName, FileFormat.CREATE);
            newfile.open();
            pgroup = (Group) newfile.getRootObject();
            type = newfile.createDatatype(Datatype.CLASS_CHAR, 1, Datatype.NATIVE, Datatype.SIGN_NONE);
            newfile.createImage(imgName, pgroup, type, dims, null, null, -1, 3, ScalarDS.INTERLACE_PIXEL, data);
            newfile.close();
        }

        // clean up memory
        data = null;
        image = null;
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
     * @throws Exception if a failure occurred
     */
    public static void saveImageAs(BufferedImage image, File file, String type) throws Exception {
        if (image == null) {
            throw new NullPointerException("The source image is null.");
        }

        ImageIO.write(image, type, file);
    }

    /**
     * Creates the gray palette of the indexed 256-color table.
     * <p>
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the gray palette in the form of byte[3][256]
     */
    public static final byte[][] createGrayPalette() {
        byte[][] p = new byte[3][256];

        for (int i = 0; i < 256; i++) {
            p[0][i] = p[1][i] = p[2][i] = (byte) (i);
        }

        return p;
    }

    /**
     * Creates the reverse gray palette of the indexed 256-color table.
     * <p>
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the gray palette in the form of byte[3][256]
     */
    public static final byte[][] createReverseGrayPalette() {
        byte[][] p = new byte[3][256];

        for (int i = 0; i < 256; i++) {
            p[0][i] = p[1][i] = p[2][i] = (byte) (255 - i);
        }

        return p;
    }

    /**
     * Creates the gray wave palette of the indexed 256-color table.
     * <p>
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the gray palette in the form of byte[3][256]
     */
    public static final byte[][] createGrayWavePalette() {
        byte[][] p = new byte[3][256];

        for (int i = 0; i < 256; i++) {
            p[0][i] = p[1][i] = p[2][i] = (byte) (255 / 2 + (255 / 2) * Math.sin((i - 32) / 20.3));
        }

        return p;
    }

    /**
     * Creates the rainbow palette of the indexed 256-color table.
     * <p>
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the rainbow palette in the form of byte[3][256]
     */
    public static final byte[][] createRainbowPalette() {
        byte r, g, b;
        byte[][] p = new byte[3][256];

        for (int i = 1; i < 255; i++) {
            if (i <= 29) {
                r = (byte) (129.36 - i * 4.36);
                g = 0;
                b = (byte) 255;
            }
            else if (i <= 86) {
                r = 0;
                g = (byte) (-133.54 + i * 4.52);
                b = (byte) 255;
            }
            else if (i <= 141) {
                r = 0;
                g = (byte) 255;
                b = (byte) (665.83 - i * 4.72);
            }
            else if (i <= 199) {
                r = (byte) (-635.26 + i * 4.47);
                g = (byte) 255;
                b = 0;
            }
            else {
                r = (byte) 255;
                g = (byte) (1166.81 - i * 4.57);
                b = 0;
            }

            p[0][i] = r;
            p[1][i] = g;
            p[2][i] = b;
        }

        p[0][0] = p[1][0] = p[2][0] = 0;
        p[0][255] = p[1][255] = p[2][255] = (byte) 255;

        return p;
    }

    /**
     * Creates the nature palette of the indexed 256-color table.
     * <p>
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the nature palette in the form of byte[3][256]
     */
    public static final byte[][] createNaturePalette() {
        byte[][] p = new byte[3][256];

        for (int i = 1; i < 210; i++) {
            p[0][i] = (byte) ((Math.sin((double) (i - 5) / 16) + 1) * 90);
            p[1][i] = (byte) ((1 - Math.sin((double) (i - 30) / 12)) * 64 * (1 - (double) i / 255) + 128 - i / 2);
            p[2][i] = (byte) ((1 - Math.sin((double) (i - 8) / 9)) * 110 + 30);
        }

        for (int i = 210; i < 255; i++) {
            p[0][i] = (byte) 80;
            p[1][i] = (byte) 0;
            p[2][i] = (byte) 200;
        }

        p[0][0] = p[1][0] = p[2][0] = 0;
        p[0][255] = p[1][255] = p[2][255] = (byte) 255;

        return p;
    }

    /**
     * Creates the wave palette of the indexed 256-color table.
     * <p>
     * The palette values are stored in a two-dimensional byte array and arrange
     * by color components of red, green and blue. palette[][] = byte[3][256],
     * where, palette[0][], palette[1][] and palette[2][] are the red, green and
     * blue components respectively.
     *
     * @return the wave palette in the form of byte[3][256]
     */
    public static final byte[][] createWavePalette() {
        byte[][] p = new byte[3][256];

        for (int i = 1; i < 255; i++) {
            p[0][i] = (byte) ((Math.sin(((double) i / 40 - 3.2)) + 1) * 128);
            p[1][i] = (byte) ((1 - Math.sin((i / 2.55 - 3.1))) * 70 + 30);
            p[2][i] = (byte) ((1 - Math.sin(((double) i / 40 - 3.1))) * 128);
        }

        p[0][0] = p[1][0] = p[2][0] = 0;
        p[0][255] = p[1][255] = p[2][255] = (byte) 255;

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
    public static final byte[][] readPalette(String filename) {
        final int COLOR256 = 256;
        BufferedReader in = null;
        String line = null;
        int nentries = 0, i, j, idx;
        float v, r, g, b, ratio, max_v, min_v, max_color, min_color;
        float[][] tbl = new float[COLOR256][4]; /* value, red, green, blue */

        if (filename == null) return null;

        try {
            in = new BufferedReader(new FileReader(filename));
        }
        catch (Exception ex) {
            log.debug("input file:", ex);
            in = null;
        }

        if (in == null) return null;

        idx = 0;
        v = r = g = b = ratio = max_v = min_v = max_color = min_color = 0;
        do {
            try {
                line = in.readLine();
            }
            catch (Exception ex) {
                log.debug("input file:", ex);
                line = null;
            }

            if (line == null) continue;

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
                max_v = min_v = v;
                max_color = min_color = r;
            }

            max_v = Math.max(max_v, v);
            max_color = Math.max(max_color, r);
            max_color = Math.max(max_color, g);
            max_color = Math.max(max_color, b);

            min_v = Math.min(min_v, v);
            min_color = Math.min(min_color, r);
            min_color = Math.min(min_color, g);
            min_color = Math.min(min_color, b);

            idx++;
            if (idx >= COLOR256) break; /* only support to 256 colors */
        } while (line != null);

        try {
            in.close();
        }
        catch (Exception ex) {
            log.debug("input file:", ex);
        }

        nentries = idx;
        if (nentries <= 1) // must have more than one entries
            return null;

        // convert color table to byte
        nentries = idx;
        if (max_color <= 1) {
            ratio = (min_color == max_color) ? 1.0f : ((COLOR256 - 1.0f) / (max_color - min_color));

            for (i = 0; i < nentries; i++) {
                for (j = 1; j < 4; j++)
                    tbl[i][j] = (tbl[i][j] - min_color) * ratio;
            }
        }

        // convert table to 256 entries
        idx = 0;
        ratio = (min_v == max_v) ? 1.0f : ((COLOR256 - 1.0f) / (max_v - min_v));

        int[][] p = new int[3][COLOR256];
        for (i = 0; i < nentries; i++) {
            idx = (int) ((tbl[i][0] - min_v) * ratio);
            for (j = 0; j < 3; j++)
                p[j][idx] = (int) tbl[i][j + 1];
        }

        /* linear interpolating missing values in the color table */
        for (i = 1; i < COLOR256; i++) {
            if ((p[0][i] + p[1][i] + p[2][i]) == 0) {
                j = i + 1;

                // figure out number of missing points between two given points
                while (j < COLOR256 && (p[0][j] + p[1][j] + p[2][j]) == 0)
                    j++;

                if (j >= COLOR256) break; // nothing in the table to interpolating

                float d1 = (p[0][j] - p[0][i - 1]) / (j - i);
                float d2 = (p[1][j] - p[1][i - 1]) / (j - i);
                float d3 = (p[2][j] - p[2][i - 1]) / (j - i);

                for (int k = i; k <= j; k++) {
                    p[0][k] = (int) (p[0][i - 1] + d1 * (k - i + 1));
                    p[1][k] = (int) (p[1][i - 1] + d2 * (k - i + 1));
                    p[2][k] = (int) (p[2][i - 1] + d3 * (k - i + 1));
                }
                i = j + 1;
            } // if ((p[0][i] + p[1][i] + p[2][i]) == 0)
        } // for (i = 1; i < COLOR256; i++) {

        byte[][] pal = new byte[3][COLOR256];
        for (i = 1; i < COLOR256; i++) {
            for (j = 0; j < 3; j++)
                pal[j][i] = (byte) (p[j][i]);
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
    public static boolean hasAlpha(Image image) {
        if (image == null) {
            return false;
        }

        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage) image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
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
    public static Image createIndexedImage(BufferedImage bufferedImage, byte[] imageData, byte[][] palette, long w, long h)
    {
        if (imageData==null || w<=0 || h<=0)
            return null;

        if (palette==null)
            palette = Tools.createGrayPalette();

        if (bufferedImage == null)
            bufferedImage = new BufferedImage((int)w, (int)h, BufferedImage.TYPE_INT_ARGB);

        final int[] pixels = ( (DataBufferInt) bufferedImage.getRaster().getDataBuffer() ).getData();
        int len = pixels.length;

        for (int i=0; i<len; i++) {
            int idx = imageData[i] & 0xff;
            int r = ((int)(palette[0][idx] & 0xff))<<16;
            int g = ((int)(palette[1][idx] & 0xff))<<8;
            int b = palette[2][idx] & 0xff;

            pixels[i] = 0xff000000 | r | g | b;
        }

        return bufferedImage;
    }

    /**
     * Creates a true color image.
     * <p>
     * DirectColorModel is used to construct the image from raw data. The
     * DirectColorModel model is similar to an X11 TrueColor visual, which has
     * the following parameters: <br>
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
     * <p>
     * The data may be arranged in one of two ways: by pixel or by plane. In
     * both cases, the dataset will have a dataspace with three dimensions,
     * height, width, and components.
     * <p>
     * For HDF4, the interlace modes specify orders for the dimensions as:
     *
     * <pre>
     * INTERLACE_PIXEL = [width][height][pixel components]
     *            INTERLACE_PLANE = [pixel components][width][height]
     * </pre>
     * <p>
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
     *            flag if the image is plane intelace.
     * @param w
     *            the width of the image.
     * @param h
     *            the height of the image.
     *
     * @return the image.
     */
    public static Image createTrueColorImage(byte[] imageData, boolean planeInterlace, long w, long h) {
        Image theImage = null;
        long imgSize = w * h;
        int packedImageData[] = new int[(int)imgSize];
        int pixel = 0, idx = 0, r = 0, g = 0, b = 0;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                pixel = r = g = b = 0;
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
                pixel = 0xff000000 | r | g | b;
                packedImageData[idx++] = pixel;
            } // for (int j=0; j<w; j++)
        } // for (int i=0; i<h; i++)

        DirectColorModel dcm = (DirectColorModel) ColorModel.getRGBdefault();
        theImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource((int)w, (int)h, dcm, packedImageData, 0, (int)w));

        packedImageData = null;

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
    public static BufferedImage toBufferedImage(Image image) {
        if (image == null) {
            return null;
        }

        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        // !!!!!!!!!!!!!!!!!! NOTICE !!!!!!!!!!!!!!!!!!!!!
        // the following way of creating a buffered image is using
        // Component.createImage(). This method can be used only if the
        // component is visible on the screen. Also, this method returns
        // buffered images that do not support transparent pixels.
        // The buffered image created by this way works for package
        // com.sun.image.codec.jpeg.*
        // It does not work well with JavaTM Advanced Imaging
        // com.sun.media.jai.codec.*;
        // if the screen setting is less than 32-bit color
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        BufferedImage bimage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bimage.createGraphics();
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
    public static byte[] getBytes(Object rawData, double[] minmax, long w, long h, boolean isTransposed, byte[] byteData) {
        return Tools.getBytes(rawData, minmax, w, h, isTransposed, null, false, byteData);
    }

    public static byte[] getBytes(Object rawData, double[] minmax, long w, long h, boolean isTransposed,
            List<Number> invalidValues, byte[] byteData) {
        return getBytes(rawData, minmax, w, h, isTransposed, invalidValues, false, byteData);
    }

    public static byte[] getBytes(Object rawData, double[] minmax, long w, long h, boolean isTransposed,
            List<Number> invalidValues, boolean convertByteData, byte[] byteData) {
        return getBytes(rawData, minmax, w, h, isTransposed,invalidValues, convertByteData, byteData, null);
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
            List<Number> invalidValues, boolean convertByteData, byte[] byteData, List<Integer> list)
    {
        double fillValue[] = null;

        // no input data
        if (rawData == null || w<=0 || h<=0) {
            return null;
        }

        // input data is not an array
        if (!rawData.getClass().isArray()) {
            return null;
        }

        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, ratio = 1.0d;
        String cname = rawData.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
        int size = Array.getLength(rawData);

        if (minmax == null) {
            minmax = new double[2];
            minmax[0] = minmax[1] = 0;
        }

        if (dname == 'B') {
            return convertByteData((byte[]) rawData, minmax, w, h, isTransposed, fillValue, convertByteData, byteData, list);
        }

        if ((byteData == null) || (size != byteData.length)) {
            byteData = new byte[size]; // reuse the old buffer
        }

        if (minmax[0] == minmax[1]) {
            Tools.findMinMax(rawData, minmax, fillValue);
        }

        min = minmax[0];
        max = minmax[1];

        if (invalidValues!=null && invalidValues.size()>0) {
            int n = invalidValues.size();
            fillValue = new double[n];
            for (int i=0; i<n; i++) {
                fillValue[i] = invalidValues.get(i).doubleValue();
            }
        }
        ratio = (min == max) ? 1.00d : (double) (255.00 / (max - min));
        long idxSrc = 0, idxDst = 0;
        switch (dname) {
            case 'S':
                short[] s = (short[]) rawData;
                for (long i = 0; i < h; i++) {
                    for (long j = 0; j < w; j++) {
                        idxSrc = idxDst =j * h + i;
                        if (isTransposed) idxDst = i * w + j;
                        byteData[(int)idxDst] = toByte(s[(int)idxSrc], ratio, min, max, fillValue, (int)idxSrc, list);
                    }
                }
                break;

            case 'I':
                int[] ia = (int[]) rawData;
                for (long i = 0; i < h; i++) {
                    for (long j = 0; j < w; j++) {
                        idxSrc = idxDst = (j * h + i);
                        if (isTransposed) idxDst = i * w + j;
                        byteData[(int)idxDst] = toByte(ia[(int)idxSrc], ratio, min, max, fillValue, (int)idxSrc, list);
                    }
                }
                break;

            case 'J':
                long[] l = (long[]) rawData;
                for (long i = 0; i < h; i++) {
                    for (long j = 0; j < w; j++) {
                        idxSrc = idxDst =j * h + i;
                        if (isTransposed) idxDst = i * w + j;
                        byteData[(int)idxDst] = toByte(l[(int)idxSrc], ratio, min, max, fillValue, (int)idxSrc, list);
                    }
                }
                break;

            case 'F':
                float[] f = (float[]) rawData;
                for (long i = 0; i < h; i++) {
                    for (long j = 0; j < w; j++) {
                        idxSrc = idxDst =j * h + i;
                        if (isTransposed) idxDst = i * w + j;
                        byteData[(int)idxDst] = toByte(f[(int)idxSrc], ratio, min, max, fillValue, (int)idxSrc, list);
                    }
                }
                break;

            case 'D':
                double[] d = (double[]) rawData;
                for (long i = 0; i < h; i++) {
                    for (long j = 0; j < w; j++) {
                        idxSrc = idxDst =j * h + i;
                        if (isTransposed) idxDst = i * w + j;
                        byteData[(int)idxDst] = toByte(d[(int)idxSrc], ratio, min, max, fillValue, (int)idxSrc, list);
                    }
                }
                break;

            default:
                byteData = null;
                break;
        } // switch (dname)

        return byteData;
    }

    private static byte toByte(double in, double ratio, double min, double max, double[] fill, int idx,  List<Integer> list)
    {
        byte out = 0;

        if (in < min || in > max || isFillValue(in, fill) || isNaNINF(in)) {
            out = 0;
            if (list!=null)
                list.add(idx);
        }
        else
            out = (byte) ((in-min)*ratio);

        return out;
    }

    private static boolean isFillValue(double in, double[] fill) {

        if (fill==null)
            return false;

        for (int i=0; i<fill.length; i++) {
            if (fill[i] == in)
                return true;
        }

        return false;
    }

    private static byte[] convertByteData(byte[] rawData, double[] minmax, long w, long h, boolean isTransposed,
            Object fillValue, boolean convertByteData, byte[] byteData, List<Integer> list) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, ratio = 1.0d;

        if (rawData == null) return null;

        if (convertByteData) {
            if (minmax[0] == minmax[1]) {
                Tools.findMinMax(rawData, minmax, fillValue);
            }
        }

        if (minmax[0] == 0 && minmax[1] == 255) convertByteData = false; // no need to convert data

        // no conversion and no transpose
        if (!convertByteData && !isTransposed) {
            if (byteData != null && byteData.length == rawData.length) {
                System.arraycopy(rawData, 0, byteData, 0, rawData.length);
                return byteData;
            }

            return rawData;
        }

        // don't want to change the original raw data
        if (byteData == null || rawData == byteData) byteData = new byte[rawData.length];

        if (!convertByteData) {
            // do not convert data, just transpose the data
            minmax[0] = 0;
            minmax[1] = 255;
            if (isTransposed) {
                for (long i = 0; i < h; i++) {
                    for (long j = 0; j < w; j++) {
                        byteData[(int)(i * w + j)] = rawData[(int)(j * h + i)];
                    }
                }
            }
            return byteData;
        }

        // special data range used, must convert the data
        min = minmax[0];
        max = minmax[1];
        ratio = (min == max) ? 1.00d : (double) (255.00 / (max - min));
        long idxSrc = 0, idxDst = 0;
        for (long i = 0; i < h; i++) {
            for (long j = 0; j < w; j++) {
                idxSrc = idxDst =j * h + i;
                if (isTransposed) idxDst = i * w + j;

                if (rawData[(int) idxSrc] > max || rawData[(int) idxSrc] < min) {
                    byteData[(int)idxDst] = (byte) 0;
                    if (list!=null)
                        list.add((int)idxSrc);
                }
                else
                    byteData[(int)idxDst] = (byte) ((rawData[(int)idxSrc] - min) * ratio);
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
    public static Object newInstance(Class<?> cls, Object[] initargs) throws Exception {
        log.trace("newInstance(Class = {}): start", cls);

        if (cls == null) {
            return null;
        }

        Object instance = null;

        if ((initargs == null) || (initargs.length == 0)) {
            instance = cls.newInstance();
        }
        else {
            Constructor<?>[] constructors = cls.getConstructors();
            if ((constructors == null) || (constructors.length == 0)) {
                return null;
            }

            boolean isConstructorMatched = false;
            Constructor<?> constructor = null;
            Class<?>[] params = null;
            int m = constructors.length;
            int n = initargs.length;
            for (int i = 0; i < m; i++) {
                constructor = constructors[i];
                params = constructor.getParameterTypes();
                if (params.length == n) {
                    // check if all the parameters are matched
                    isConstructorMatched = params[0].isInstance(initargs[0]);
                    for (int j = 0; j < n; j++) {
                        isConstructorMatched = isConstructorMatched && params[j].isInstance(initargs[j]);
                    }

                    if (isConstructorMatched) {
                        try {
                            instance = constructor.newInstance(initargs);
                        } catch (Exception ex) {
                            log.debug("Error creating instance of {}: {}", cls, ex.getMessage());
                            ex.printStackTrace();
                        }
                        break;
                    }
                }
            } // for (int i=0; i<m; i++) {
        }
        log.trace("newInstance(Class = {}): finish", cls);

        return instance;
    }

    /**
     * Computes autocontrast parameters (gain equates to contrast and bias
     * equates to brightness) for integers.
     * <p>
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
    public static int autoContrastCompute(Object data, double[] params, boolean isUnsigned) {
        int retval = 1;
        long maxDataValue = 255;
        double[] minmax = new double[2];

        // check parameters
        if ((data == null) || (params == null) || (Array.getLength(data) <= 0) || (params.length < 2)) {
            return -1;
        }

        retval = autoContrastComputeMinMax(data, minmax);

        // force the min_max method so we can look at the target grids data sets
        if ((retval < 0) || (minmax[1] - minmax[0] < 10)) {
            retval = findMinMax(data, minmax, null);
        }

        if (retval < 0) {
            return -1;
        }

        String cname = data.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
        switch (dname) {
            case 'B':
                maxDataValue = MAX_INT8;
                break;
            case 'S':
                maxDataValue = MAX_INT16;
                if (isUnsigned) {
                    maxDataValue = MAX_UINT8; // data was upgraded from unsigned byte
                }
                break;
            case 'I':
                maxDataValue = MAX_INT32;
                if (isUnsigned) {
                    maxDataValue = MAX_UINT16; // data was upgraded from unsigned short
                }
                break;
            case 'J':
                maxDataValue = MAX_INT64;
                if (isUnsigned) {
                    maxDataValue = MAX_UINT32; // data was upgraded from unsigned int
                }
                break;
            default:
                retval = -1;
                break;
        } // switch (dname)

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
            double diff = minmax[1] - minmax[0];
            double newmax = (minmax[1] + (diff * 0.1));
            double newmin = (minmax[0] - (diff * 0.1));

            if (newmax <= maxDataValue) {
                minmax[1] = newmax;
            }

            if (newmin >= 0) {
                minmax[0] = newmin;
            }

            params[0] = maxDataValue / (minmax[1] - minmax[0]);
            params[1] = -minmax[0];
        }

        return retval;
    }

    /**
     * Apply autocontrast parameters to the original data in place (destructive)
     *
     * @param data_in
     *            the original data array of signed/unsigned integers
     * @param data_out
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
    public static Object autoContrastApply(Object data_in, Object data_out, double[] params, double[] minmax,
            boolean isUnsigned) {
        int size = 0;
        double min = -MAX_INT64, max = MAX_INT64;

        if ((data_in == null) || (params == null) || (params.length < 2)) {
            return null;
        }

        if (minmax != null) {
            min = minmax[0];
            max = minmax[1];
        }
        // input and output array must be the same size
        size = Array.getLength(data_in);
        if ((data_out != null) && (size != Array.getLength(data_out))) {
            return null;
        }

        double gain = params[0];
        double bias = params[1];
        double value_out, value_in;
        String cname = data_in.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);

        switch (dname) {
            case 'B':
                byte[] b_in = (byte[]) data_in;
                if (data_out == null) {
                    data_out = new byte[size];
                }
                byte[] b_out = (byte[]) data_out;
                byte b_max = (byte) MAX_INT8;

                for (int i = 0; i < size; i++) {
                    value_in = Math.max(b_in[i], min);
                    value_in = Math.min(b_in[i], max);
                    value_out = (value_in + bias) * gain;
                    value_out = Math.max(value_out, 0.0);
                    value_out = Math.min(value_out, b_max);
                    b_out[i] = (byte) value_out;
                }
                break;
            case 'S':
                short[] s_in = (short[]) data_in;
                if (data_out == null) {
                    data_out = new short[size];
                }
                short[] s_out = (short[]) data_out;
                short s_max = (short) MAX_INT16;

                if (isUnsigned) {
                    s_max = (short) MAX_UINT8; // data was upgraded from unsigned byte
                }

                for (int i = 0; i < size; i++) {
                    value_in = Math.max(s_in[i], min);
                    value_in = Math.min(s_in[i], max);
                    value_out = (value_in + bias) * gain;
                    value_out = Math.max(value_out, 0.0);
                    value_out = Math.min(value_out, s_max);
                    s_out[i] = (byte) value_out;
                }
                break;
            case 'I':
                int[] i_in = (int[]) data_in;
                if (data_out == null) {
                    data_out = new int[size];
                }
                int[] i_out = (int[]) data_out;
                int i_max = (int) MAX_INT32;
                if (isUnsigned) {
                    i_max = (int) MAX_UINT16; // data was upgraded from unsigned short
                }

                for (int i = 0; i < size; i++) {
                    value_in = Math.max(i_in[i], min);
                    value_in = Math.min(i_in[i], max);
                    value_out = (value_in + bias) * gain;
                    value_out = Math.max(value_out, 0.0);
                    value_out = Math.min(value_out, i_max);
                    i_out[i] = (byte) value_out;
                }
                break;
            case 'J':
                long[] l_in = (long[]) data_in;
                if (data_out == null) {
                    data_out = new long[size];
                }
                long[] l_out = (long[]) data_out;
                long l_max = MAX_INT64;
                if (isUnsigned) {
                    l_max = MAX_UINT32; // data was upgraded from unsigned int
                }

                for (int i = 0; i < size; i++) {
                    value_in = Math.max(l_in[i], min);
                    value_in = Math.min(l_in[i], max);
                    value_out = (value_in + bias) * gain;
                    value_out = Math.max(value_out, 0.0);
                    value_out = Math.min(value_out, l_max);
                    l_out[i] = (byte) value_out;
                }
                break;
            default:
                break;
        } // switch (dname)

        return data_out;
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
    public static int autoContrastConvertImageBuffer(Object src, byte[] dst, boolean isUnsigned) {
        int retval = 0;

        if ((src == null) || (dst == null) || (dst.length != Array.getLength(src))) {
            return -1;
        }

        int size = dst.length;
        String cname = src.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
        switch (dname) {
            case 'B':
                byte[] b_src = (byte[]) src;
                if (isUnsigned) {
                    for (int i = 0; i < size; i++) {
                        dst[i] = b_src[i];
                    }
                }
                else {
                    for (int i = 0; i < size; i++) {
                        dst[i] = (byte) ((b_src[i] & 0x7F) << 1);
                    }
                }
                break;
            case 'S':
                short[] s_src = (short[]) src;
                if (isUnsigned) { // data was upgraded from unsigned byte
                    for (int i = 0; i < size; i++) {
                        dst[i] = (byte) s_src[i];
                    }
                }
                else {
                    for (int i = 0; i < size; i++) {
                        dst[i] = (byte) ((s_src[i] >> 7) & 0xFF);
                    }
                }
                break;
            case 'I':
                int[] i_src = (int[]) src;
                if (isUnsigned) { // data was upgraded from unsigned short
                    for (int i = 0; i < size; i++) {
                        dst[i] = (byte) ((i_src[i] >> 8) & 0xFF);
                    }
                }
                else {
                    for (int i = 0; i < size; i++) {
                        dst[i] = (byte) ((i_src[i] >> 23) & 0xFF);
                    }
                }
                break;
            case 'J':
                long[] l_src = (long[]) src;
                if (isUnsigned) { // data was upgraded from unsigned int
                    for (int i = 0; i < size; i++) {
                        dst[i] = (byte) ((l_src[i] >> 24) & 0xFF);
                    }
                }
                else {
                    for (int i = 0; i < size; i++) {
                        dst[i] = (byte) ((l_src[i] >> 55) & 0xFF);
                    }
                }
                break;
            default:
                retval = -1;
                break;
        } // switch (dname)

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
    public static int autoContrastComputeMinMax(Object data, double[] minmax) {
        int retval = 1;

        if ((data == null) || (minmax == null) || (Array.getLength(data) <= 0) || (Array.getLength(minmax) < 2)) {
            return -1;
        }

        double[] avgstd = { 0, 0 };
        retval = computeStatistics(data, avgstd, null);
        if (retval < 0) {
            return retval;
        }

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
    public static int findMinMax(Object data, double[] minmax, Object fillValue) {
        int retval = 1;

        if ((data == null) || (minmax == null) || (Array.getLength(data) <= 0) || (Array.getLength(minmax) < 2)) {
            return -1;
        }

        int n = Array.getLength(data);
        double fill = 0.0;
        boolean hasFillValue = (fillValue != null && fillValue.getClass().isArray());

        String cname = data.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
        log.trace("findMinMax() cname={} : dname={}", cname, dname);

        minmax[0] = Float.MAX_VALUE;
        minmax[1] = -Float.MAX_VALUE;

        switch (dname) {
            case 'B':
                byte[] b = (byte[]) data;
                minmax[0] = minmax[1] = b[0];

                if (hasFillValue) fill = ((byte[]) fillValue)[0];
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && b[i] == fill) continue;
                    if (minmax[0] > b[i]) {
                        minmax[0] = b[i];
                    }
                    if (minmax[1] < b[i]) {
                        minmax[1] = b[i];
                    }
                }
                break;
            case 'S':
                short[] s = (short[]) data;
                minmax[0] = minmax[1] = s[0];

                if (hasFillValue) fill = ((short[]) fillValue)[0];

                for (int i = 0; i < n; i++) {
                    if (hasFillValue && s[i] == fill) continue;
                    if (minmax[0] > s[i]) {
                        minmax[0] = s[i];
                    }
                    if (minmax[1] < s[i]) {
                        minmax[1] = s[i];
                    }
                }
                break;
            case 'I':
                int[] ia = (int[]) data;
                minmax[0] = minmax[1] = ia[0];

                if (hasFillValue) fill = ((int[]) fillValue)[0];

                for (int i = 0; i < n; i++) {
                    if (hasFillValue && ia[i] == fill) continue;
                    if (minmax[0] > ia[i]) {
                        minmax[0] = ia[i];
                    }
                    if (minmax[1] < ia[i]) {
                        minmax[1] = ia[i];
                    }
                }
                break;
            case 'J':
                long[] l = (long[]) data;
                minmax[0] = minmax[1] = l[0];

                if (hasFillValue) fill = ((long[]) fillValue)[0];
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && l[i] == fill) continue;
                    if (minmax[0] > l[i]) {
                        minmax[0] = l[i];
                    }
                    if (minmax[1] < l[i]) {
                        minmax[1] = l[i];
                    }
                }
                break;
            case 'F':
                float[] f = (float[]) data;
                minmax[0] = minmax[1] = f[0];

                if (hasFillValue) fill = ((float[]) fillValue)[0];
                for (int i = 0; i < n; i++) {
                    if ((hasFillValue && f[i] == fill) || isNaNINF((double) f[i])) continue;
                    if (minmax[0] > f[i]) {
                        minmax[0] = f[i];
                    }
                    if (minmax[1] < f[i]) {
                        minmax[1] = f[i];
                    }
                }

                break;
            case 'D':
                double[] d = (double[]) data;
                minmax[0] = minmax[1] = d[0];

                if (hasFillValue) fill = ((double[]) fillValue)[0];
                for (int i = 0; i < n; i++) {
                    if ((hasFillValue && d[i] == fill) || isNaNINF(d[i])) continue;

                    if (minmax[0] > d[i]) {
                        minmax[0] = d[i];
                    }
                    if (minmax[1] < d[i]) {
                        minmax[1] = d[i];
                    }
                }
                break;
            default:
                retval = -1;
                break;
        } // switch (dname)

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
    public static int findDataDist(Object data, int[] dataDist, double[] minmax) {
        int retval = 0;
        double delt = 1;

        if ((data == null) || (minmax == null) || dataDist == null) return -1;

        int n = Array.getLength(data);

        if (minmax[1] != minmax[0]) delt = (dataDist.length - 1) / (minmax[1] - minmax[0]);

        for (int i = 0; i < dataDist.length; i++)
            dataDist[i] = 0;

        int idx;
        double val;
        for (int i = 0; i < n; i++) {
            val = ((Number) Array.get(data, i)).doubleValue();
            if (val>=minmax[0] && val <=minmax[1]) {
                idx = (int) ((val - minmax[0]) * delt);
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
    public static int computeStatistics(Object data, double[] avgstd, Object fillValue) {
        int retval = 1, npoints = 0;
        double sum = 0, avg = 0.0, var = 0.0, diff = 0.0, fill = 0.0;

        if ((data == null) || (avgstd == null) || (Array.getLength(data) <= 0) || (Array.getLength(avgstd) < 2)) {
            return -1;
        }

        int n = Array.getLength(data);
        boolean hasFillValue = (fillValue != null && fillValue.getClass().isArray());

        String cname = data.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf("[") + 1);
        log.trace("computeStatistics() cname={} : dname={}", cname, dname);

        npoints = 0;
        switch (dname) {
            case 'B':
                byte[] b = (byte[]) data;
                if (hasFillValue) fill = ((byte[]) fillValue)[0];
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && b[i] == fill) continue;
                    sum += b[i];
                    npoints++;
                }
                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && b[i] == fill) continue;
                    diff = b[i] - avg;
                    var += diff * diff;
                }
                break;
            case 'S':
                short[] s = (short[]) data;
                if (hasFillValue) fill = ((short[]) fillValue)[0];
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && s[i] == fill) continue;
                    sum += s[i];
                    npoints++;
                }
                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && s[i] == fill) continue;
                    diff = s[i] - avg;
                    var += diff * diff;
                }
                break;
            case 'I':
                int[] ia = (int[]) data;
                if (hasFillValue) fill = ((int[]) fillValue)[0];
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && ia[i] == fill) continue;
                    sum += ia[i];
                    npoints++;
                }
                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && ia[i] == fill) continue;
                    diff = ia[i] - avg;
                    var += diff * diff;
                }
                break;
            case 'J':
                long[] l = (long[]) data;
                if (hasFillValue) fill = ((long[]) fillValue)[0];
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && l[i] == fill) continue;
                    sum += l[i];
                    npoints++;
                }

                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && l[i] == fill) continue;
                    diff = l[i] - avg;
                    var += diff * diff;
                }
                break;
            case 'F':
                float[] f = (float[]) data;
                if (hasFillValue) fill = ((float[]) fillValue)[0];
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && f[i] == fill) continue;
                    sum += f[i];
                    npoints++;
                }

                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && f[i] == fill) continue;
                    diff = f[i] - avg;
                    var += diff * diff;
                }
                break;
            case 'D':
                double[] d = (double[]) data;
                if (hasFillValue) fill = ((double[]) fillValue)[0];
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && d[i] == fill) continue;
                    sum += d[i];
                    npoints++;
                }
                avg = sum / npoints;
                for (int i = 0; i < n; i++) {
                    if (hasFillValue && d[i] == fill) continue;
                    diff = d[i] - avg;
                    var += diff * diff;
                }
                break;
            default:
                retval = -1;
                break;
        } // switch (dname)

        if (npoints <= 1) {
            if (npoints < 1) avgstd[0] = fill;
            avgstd[1] = 0;
        }
        else {
            avgstd[0] = avg;
            avgstd[1] = Math.sqrt(var / (npoints - 1));
        }

        return retval;
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
    public static final String toBinaryString(long v, int nbytes) {
        if (nbytes <= 0) return null;

        int nhex = nbytes * 2;
        short[] hex = new short[nhex];

        for (int i = 0; i < nhex; i++)
            hex[i] = (short) (0x0F & (v >> (i * 4)));

        StringBuffer sb = new StringBuffer();
        boolean isEven = true;
        for (int i = nhex - 1; i >= 0; i--) {
            if (isEven) sb.append(" ");
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
            }
        }

        return sb.toString();
    }

    final static char[] HEXCHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

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
    public static final String toHexString (long v, int nbytes) {
        if (nbytes <= 0) return null;

        int nhex = nbytes * 2;
        short[] hex = new short[nhex];

        for (int i = 0; i < nhex; i++) {
            hex[i] = (short) (0x0F & (v >> (i * 4)));
        }

        StringBuffer sb = new StringBuffer();
        for (int i = nhex - 1; i >= 0; i--) {
            sb.append(HEXCHARS[hex[i]]);
        }

        return sb.toString();
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
     * @return true if bitmask is applied successfuly; otherwise, false.
     */
    public static final boolean applyBitmask(Object theData, BitSet theMask, ViewProperties.BITMASK_OP op) {
        if (theData == null || Array.getLength(theData) <= 0 || theMask == null) return false;

        char nt = '0';
        String cName = theData.getClass().getName();
        int cIndex = cName.lastIndexOf("[");
        if (cIndex >= 0) {
            nt = cName.charAt(cIndex + 1);
        }

        // only deal with 8/16/32/64 bit datasets
        if (!(nt == 'B' || nt == 'S' || nt == 'I' || nt == 'J')) return false;

        long bmask = 0, theValue = 0, packedValue = 0, bitValue = 0;

        int nbits = theMask.length();
        int len = Array.getLength(theData);

        for (int i = 0; i < nbits; i++) {
            if (theMask.get(i)) bmask += 1 << i;
        }

        for (int i = 0; i < len; i++) {
            if (nt == 'B')
                theValue = ((byte[]) theData)[i] & bmask;
            else if (nt == 'S')
                theValue = ((short[]) theData)[i] & bmask;
            else if (nt == 'I')
                theValue = ((int[]) theData)[i] & bmask;
            else if (nt == 'J')
                theValue = ((long[]) theData)[i] & bmask;

            // apply bitmask only
            if (op == BITMASK_OP.AND)
                packedValue = theValue;
            else {
                // extract bits
                packedValue = 0;
                int bitPosition = 0;
                bitValue = 0;

                for (int j = 0; j < nbits; j++) {
                    if (theMask.get(j)) {
                        bitValue = (theValue & 1);
                        packedValue += (bitValue << bitPosition);
                        bitPosition++;
                    }
                    // move to the next bit
                    theValue = theValue >> 1;
                }
            }

            if (nt == 'B')
                ((byte[]) theData)[i] = (byte) packedValue;
            else if (nt == 'S')
                ((short[]) theData)[i] = (short) packedValue;
            else if (nt == 'I')
                ((int[]) theData)[i] = (int) packedValue;
            else if (nt == 'J')
                ((long[]) theData)[i] = packedValue;
        } /* for (int i = 0; i < len; i++) */

        return true;
    } /* public static final boolean applyBitmask() */
    
    /**
     * Read HDF5 user block data into byte array.
     *
     * @param filename the HDF5 file from which to get the user block
     *
     * @return a byte array of user block, or null if there is user data.
     */
    public static byte[] getHDF5UserBlock(String filename) {
        byte[] userBlock = null;
        RandomAccessFile raf = null;

        try {
            raf = new RandomAccessFile(filename, "r");
        }
        catch (Exception ex) {
            try {
                raf.close();
            }
            catch (Throwable err) {
                ;
            }
            raf = null;
        }

        if (raf == null) {
            return null;
        }

        byte[] header = new byte[8];
        long fileSize = 0;
        try {
            fileSize = raf.length();
        }
        catch (Exception ex) {
            fileSize = 0;
        }
        if (fileSize <= 0) {
            try {
                raf.close();
            }
            catch (Throwable err) {
                ;
            }
            return null;
        }

        // The super block is located by searching for the HDF5 file signature
        // at byte offset 0, byte offset 512 and at successive locations in the
        // file, each a multiple of two of the previous location, i.e. 0, 512,
        // 1024, 2048, etc
        long offset = 0;
        boolean ish5 = false;
        while (offset < fileSize) {
            try {
                raf.seek(offset);
                raf.read(header);
            }
            catch (Exception ex) {
                header = null;
            }

            if ((header[0] == -119) && (header[1] == 72) && (header[2] == 68)
                    && (header[3] == 70) && (header[4] == 13)
                    && (header[5] == 10) && (header[6] == 26)
                    && (header[7] == 10)) {
                ish5 = true;
                break; // find the end of user block
            }
            else {
                ish5 = false;
                if (offset == 0) {
                    offset = 512;
                }
                else {
                    offset *= 2;
                }
            }
        }

        if (!ish5 || (offset == 0)) {
            try {
                raf.close();
            }
            catch (Throwable err) {
                ;
            }
            return null;
        }

        int blockSize = (int) offset;
        userBlock = new byte[blockSize];
        try {
            raf.seek(0);
            raf.read(userBlock, 0, blockSize);
        }
        catch (Exception ex) {
            userBlock = null;
        }

        try {
            raf.close();
        }
        catch (Exception ex) {
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
    public static boolean setHDF5UserBlock(String fin, String fout, byte[] buf) {
        boolean ish5 = false;

        if ((buf == null) || (buf.length <= 0)) {
            return false;
        }

        File tmpFile = new File(fin);
        if (!tmpFile.exists()) {
            return false;
        }

        // find the end of user block for the input file;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(fin, "r");
        }
        catch (Exception ex) {
            raf = null;
        }

        if (raf == null) {
            return false;
        }

        byte[] header = new byte[8];
        long fileSize = 0;
        try {
            fileSize = raf.length();
        }
        catch (Exception ex) {
            fileSize = 0;
        }
        try {
            fileSize = raf.length();
        }
        catch (Exception ex) {
            fileSize = 0;
        }
        if (fileSize <= 0) {
            try {
                raf.close();
            }
            catch (Throwable err) {
                ;
            }
            return false;
        }

        // The super block is located by searching for the HDF5 file signature
        // at byte offset 0, byte offset 512 and at successive locations in the
        // file, each a multiple of two of the previous location, i.e. 0, 512,
        // 1024, 2048, etc
        long offset = 0;
        while (offset < fileSize) {
            try {
                raf.seek(offset);
                raf.read(header);
            }
            catch (Exception ex) {
                header = null;
            }

            if ((header[0] == -119) && (header[1] == 72) && (header[2] == 68)
                    && (header[3] == 70) && (header[4] == 13)
                    && (header[5] == 10) && (header[6] == 26)
                    && (header[7] == 10)) {
                ish5 = true;
                break;
            }
            else {
                ish5 = false;
                if (offset == 0) {
                    offset = 512;
                }
                else {
                    offset *= 2;
                }
            }
        }
        try {
            raf.close();
        }
        catch (Throwable err) {
            ;
        }

        if (!ish5) {
            return false;
        }

        int length = 0;
        int bsize = 1024;
        byte[] buffer;
        BufferedInputStream bi = null;
        BufferedOutputStream bo = null;

        try {
            bi = new BufferedInputStream(new FileInputStream(fin));
        }
        catch (Exception ex) {
            try {
                bi.close();
            }
            catch (Exception ex2) {
            }
            return false;
        }

        try {
            bo = new BufferedOutputStream(new FileOutputStream(fout));
        }
        catch (Exception ex) {
            try {
                bo.close();
            }
            catch (Exception ex2) {
            }
            try {
                bi.close();
            }
            catch (Exception ex2) {
            }
            return false;
        }

        // skip the header of original file
        try {
            bi.skip(offset);
        }
        catch (Exception ex) {
        }

        // write the header into the new file
        try {
            bo.write(buf, 0, buf.length);
        }
        catch (Exception ex) {
        }

        // The super block space is allocated by offset 0, 512, 1024, 2048, etc
        offset = 512;
        while (offset < buf.length) {
            offset *= 2;
        }
        int padSize = (int) (offset - buf.length);
        if (padSize > 0) {
            byte[] padBuf = new byte[padSize];
            try {
                bo.write(padBuf, 0, padSize);
            }
            catch (Exception ex) {
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
        }
        try {
            bi.close();
        }
        catch (Exception ex) {
        }
        try {
            bo.close();
        }
        catch (Exception ex) {
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
    public static boolean isHDF4(String filename) {
        boolean ish4 = false;
        RandomAccessFile raf = null;

        try {
            raf = new RandomAccessFile(filename, "r");
        }
        catch (Exception ex) {
            raf = null;
        }

        if (raf == null) {
            return false;
        }

        byte[] header = new byte[4];
        try {
            raf.read(header);
        }
        catch (Exception ex) {
            header = null;
        }

        if (header != null) {
            if (
            // HDF4
            ((header[0] == 14) && (header[1] == 3) && (header[2] == 19) && (header[3] == 1))
            /*
             * // netCDF || (header[0]==67 && header[1]==68 && header[2]==70 &&
             * header[3]==1)
             */
            ) {
                ish4 = true;
            }
            else {
                ish4 = false;
            }
        }

        try {
            raf.close();
        }
        catch (Exception ex) {
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
    public static boolean isHDF5(String filename) {
        boolean ish5 = false;
        RandomAccessFile raf = null;

        try {
            raf = new RandomAccessFile(filename, "r");
        }
        catch (Exception ex) {
            raf = null;
        }

        if (raf == null) {
            return false;
        }

        byte[] header = new byte[8];
        long fileSize = 0;
        try {
            fileSize = raf.length();
        }
        catch (Exception ex) {
        }

        // The super block is located by searching for the HDF5 file signature
        // at byte offset 0, byte offset 512 and at successive locations in the
        // file, each a multiple of two of the previous location, i.e. 0, 512,
        // 1024, 2048, etc
        long offset = 0;
        while (!ish5 && (offset < fileSize)) {
            try {
                raf.seek(offset);
                raf.read(header);
            }
            catch (Exception ex) {
                header = null;
            }

            if ((header[0] == -119) && (header[1] == 72) && (header[2] == 68)
                    && (header[3] == 70) && (header[4] == 13)
                    && (header[5] == 10) && (header[6] == 26)
                    && (header[7] == 10)) {
                ish5 = true;
            }
            else {
                ish5 = false;
                if (offset == 0) {
                    offset = 512;
                }
                else {
                    offset *= 2;
                }
            }
        }

        try {
            raf.close();
        }
        catch (Exception ex) {
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
    public static boolean isNetcdf(String filename) {
        boolean isnc = false;
        RandomAccessFile raf = null;

        try {
            raf = new RandomAccessFile(filename, "r");
        }
        catch (Exception ex) {
            raf = null;
        }

        if (raf == null) {
            return false;
        }

        byte[] header = new byte[4];
        try {
            raf.read(header);
        }
        catch (Exception ex) {
            header = null;
        }

        if (header != null) {
            if (
            // netCDF
            (header[0] == 67) && (header[1] == 68) && (header[2] == 70)
                    && (header[3] == 1)) {
                isnc = true;
            }
            else {
                isnc = false;
            }
        }

        try {
            raf.close();
        }
        catch (Exception ex) {
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
    public static final void launchBrowser(String url) throws Exception {
        String os = System.getProperty("os.name");
        Runtime runtime = Runtime.getRuntime();

        // Block for Windows Platform
        if (os.startsWith("Windows")) {
            String cmd = "rundll32 url.dll,FileProtocolHandler " + url;

            if (new File(url).exists()) cmd = "cmd /c start \"\" \"" + url + "\"";
            runtime.exec(cmd);
        }
        // Block for Mac OS
        else if (os.startsWith("Mac OS")) {
            Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
            Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });

            if (new File(url).exists()) {
                // local file
                url = "file://" + url;
            }
            openURL.invoke(null, new Object[] { url });
        }
        // Block for UNIX Platform
        else {
            String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
            String browser = null;
            for (int count = 0; count < browsers.length && browser == null; count++)
                if (runtime.exec(new String[] { "which", browsers[count] }).waitFor() == 0) browser = browsers[count];
            if (browser == null)
                throw new Exception("Could not find web browser");
            else
                runtime.exec(new String[] { browser, url });
        }
    } /* public static final void launchBrowser(String url) */

    /** Create a new HDF file with default file creation properties
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
    public static FileFormat createNewFile(String filename, String dir,
            String type, List<FileFormat> openFiles) throws Exception {
        File f = new File(filename);

        String fname = f.getAbsolutePath();
        if (fname == null) return null;

        fname = fname.trim();
        if ((fname == null) || (fname.length() == 0)) {
            throw new Exception("Invalid file name.");
        }

        String extensions = FileFormat.getFileExtensions();
        boolean noExtension = true;
        if ((extensions != null) && (extensions.length() > 0)) {
            java.util.StringTokenizer currentExt = new java.util.StringTokenizer(extensions, ",");
            String extension = "";
            String tmpFilename = fname.toLowerCase();
            while (currentExt.hasMoreTokens() && noExtension) {
                extension = currentExt.nextToken().trim().toLowerCase();
                noExtension = !tmpFilename.endsWith("." + extension);
            }
        }

        if (noExtension) {
            if (type == FileFormat.FILE_TYPE_HDF4) {
                fname += ".hdf";
                f = new File(fname);
                //setSelectedFile(f);
            }
            else if (type == FileFormat.FILE_TYPE_HDF5) {
                fname += ".h5";
                f = new File(fname);
                //setSelectedFile(f);
            }
        }

        if (f.exists() && f.isDirectory()) {
            throw new Exception("File is a directory.");
        }

        File pfile = f.getParentFile();
        if (pfile == null) {
            fname = dir + File.separator + fname;
            f = new File(fname);
        }
        else if (!pfile.exists()) {
            throw new Exception("File path does not exist at\n" + pfile.getPath());
        }

        // check if the file is in use
        if (openFiles != null) {
            FileFormat theFile = null;
            Iterator<FileFormat> iterator = openFiles.iterator();
            while (iterator.hasNext()) {
                theFile = (FileFormat) iterator.next();
                if (theFile.getFilePath().equals(fname)) {
                    throw new Exception("Unable to create the new file. \nThe file is being used.");
                }
            }
        }

        if (f.exists()) {
            MessageBox confirm = new MessageBox(Display.getCurrent().getActiveShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            confirm.setText(Display.getCurrent().getActiveShell().getText());
            confirm.setMessage("File exists. Do you want to replace it?");
            if (confirm.open() == SWT.NO) return null;
        }

        try {
            int aFlag = FileFormat.FILE_CREATE_DELETE;
            if (ViewProperties.isEarlyLib())
                aFlag = FileFormat.FILE_CREATE_DELETE | FileFormat.FILE_CREATE_EARLY_LIB;
            FileFormat theFile = FileFormat.getFileFormat(type).createFile(fname, aFlag);
            return theFile;
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
    public static final File checkNewFile(String path, String ext) {
        File file = new File(path + "new" + ext);
        int i = 1;

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
     *            the nubmer to be checked
     *
     * @return true if the number is Nan or INF; otherwise, false.
     */
    public static final boolean isNaNINF(double val) {
        if (Double.isNaN(val) || val == Float.NEGATIVE_INFINITY || val == Float.POSITIVE_INFINITY
                || val == Double.NEGATIVE_INFINITY || val == Double.POSITIVE_INFINITY) return true;

        return false;
    }
    
    /** 
     * Show an SWT error dialog with the given error message.
     * @param parent
     * @param errorMsg
     * @param title
     */
    public static void showError(Shell parent, String errorMsg, String title) {
        MessageBox error = new MessageBox(parent, SWT.ICON_ERROR | SWT.OK);
        error.setText(title);
        error.setMessage(errorMsg);
        error.open();
    }
}
