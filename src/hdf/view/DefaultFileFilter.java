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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.swing.filechooser.FileFilter;

/**
 * A convenience implementation of FileFilter that filters out all files except
 * for those type extensions that it knows about.
 *
 * @author Peter X. Cao
 * @version 2.4 9/6/2007
 */
public class DefaultFileFilter extends FileFilter {
    private static FileFilter FILE_FILTER_HDF = null;
    private static FileFilter FILE_FILTER_HDF4 = null;
    private static FileFilter FILE_FILTER_HDF5 = null;
    private static FileFilter FILE_FILTER_JPEG = null;
    private static FileFilter FILE_FILTER_TIFF = null;
    private static FileFilter FILE_FILTER_PNG = null;
    private static FileFilter FILE_FILTER_GIF = null;
    private static FileFilter FILE_FILTER_BMP = null;
    private static FileFilter FILE_FILTER_IMG = null;
    private static FileFilter FILE_FILTER_TEXT = null;
    private static FileFilter FILE_FILTER_BINARY = null;

    private static String fileExtension = ViewProperties.getFileExtension();

    private Hashtable<String, DefaultFileFilter> filters = null;
    private String description = null;
    private String fullDescription = null;
    private boolean useExtensionsInDescription = true;

    /**
     * Creates a file filter. If no filters are added, then all files are
     * accepted.
     *
     * @see #addExtension
     */
    public DefaultFileFilter() {
        this.filters = new Hashtable<String, DefaultFileFilter>();
    }

    /**
     * Creates a file filter that accepts files with the given extension.
     * Example: new DefaultFileFilter("jpg");
     *
     * @see #addExtension
     *
     * @param extension the file extension to filter on
     */
    public DefaultFileFilter(String extension) {
        this(extension, null);
    }

    /**
     * Creates a file filter that accepts the given file type. Example: new
     * DefaultFileFilter("jpg", "JPEG Image Images");
     *
     * Note that the "." before the extension is not needed. If provided, it
     * will be ignored.
     *
     * @see #addExtension
     *
     * @param extension the file extension to filter on
     * @param description the file extension full description
     */
    public DefaultFileFilter(String extension, String description) {
        this();
        if (extension != null) {
            addExtension(extension);
        }
        if (description != null) {
            setDescription(description);
        }
    }

    /**
     * Creates a file filter from the given string array. Example: new
     * DefaultFileFilter(String {"gif", "jpg"});
     *
     * Note that the "." before the extension is not needed adn will be ignored.
     *
     * @see #addExtension
     *
     * @param filters
     *          the list of filter names
     */
    public DefaultFileFilter(String[] filters) {
        this(filters, null);
    }

    /**
     * Creates a file filter from the given string array and description.
     * Example: new DefaultFileFilter(String {"gif", "jpg"},
     * "Gif and JPG Images");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     *
     * @see #addExtension
     *
     * @param filters
     *          the list of filter names
     * @param description
     *          the name of the filter list
     */
    public DefaultFileFilter(String[] filters, String description) {
        this();
        for (int i = 0; i < filters.length; i++) {
            // add filters one by one
            addExtension(filters[i]);
        }
        if (description != null) {
            setDescription(description);
        }
    }

    /**
     * @param f the file to be accepted
     *
     * @return true if this file should be shown in the directory pane, false if
     * it shouldn't.
     *
     * Files that begin with "." are ignored.
     *
     * @see #getExtension
     */
    @Override
    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            String extension = getExtension(f);
            if ((extension != null) && (filters.get(getExtension(f)) != null)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param f the file under query
     *
     * @return the extension portion of the file's name .
     *
     * @see #getExtension
     * @see FileFilter#accept
     */
    public String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if ((i > 0) && (i < filename.length() - 1)) {
                return filename.substring(i + 1).toLowerCase();
            }
        }
        return null;
    }

    /**
     * Adds a filetype "dot" extension to filter against.
     *
     * For example: the following code will create a filter that filters out all
     * files except those that end in ".jpg" and ".tif":
     *
     * DefaultFileFilter filter = new DefaultFileFilter();
     * filter.addExtension("jpg"); filter.addExtension("tif"); or
     * filter.addExtension("jpg, tif");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     *
     * @param extension the file extension to add to the file filter
     */
    public void addExtension(String extension) {
        if (filters == null) {
            filters = new Hashtable<String, DefaultFileFilter>(5);
        }

        String ext = null;
        StringTokenizer st = new StringTokenizer(extension, ",");
        while (st.hasMoreElements()) {
            ext = st.nextToken().trim();
            filters.put(ext.toLowerCase(), this);
        }
        fullDescription = null;
    }

    /**
     * @return the human readable description of this filter. For example:
     * "JPEG and GIF Image Files (*.jpg, *.gif)"
     */
    @Override
    public String getDescription() {
        if (fullDescription == null) {
            if ((description == null) || isExtensionListInDescription()) {
                fullDescription = description == null ? "(" : description
                        + " (";
                // build the description from the extension list
                Enumeration<String> extensions = filters.keys();
                if (extensions != null) {

                    if (!extensions.hasMoreElements()) {
                        fullDescription = null;
                        return null;
                    }

                    fullDescription += "." + extensions.nextElement();
                    while (extensions.hasMoreElements()) {
                        fullDescription += ", "
                                + extensions.nextElement();
                    }
                }
                fullDescription += ")";
            }
            else {
                fullDescription = description;
            }
        }
        return fullDescription;
    }

    /**
     * Sets the human readable description of this filter. For example:
     * filter.setDescription("Gif and JPG Images");
     *
     * @param description the full description of the file filter
     */
    public void setDescription(String description) {
        this.description = description;
        fullDescription = null;
    }

    /**
     * Determines whether the extension list (.jpg, .gif, etc) should show up in
     * the human readable description.
     *
     * Only relevent if a description was provided in the constructor or using
     * setDescription();
     *
     * @param b the show state of the extension list
     */
    public void setExtensionListInDescription(boolean b) {
        useExtensionsInDescription = b;
        fullDescription = null;
    }

    /**
     * @return whether the extension list (.jpg, .gif, etc) should show up in
     * the human readable description.
     *
     * Only relevent if a description was provided in the constructor or using
     * setDescription();
     */
    public boolean isExtensionListInDescription() {
        return useExtensionsInDescription;
    }

    /** @return a file filter for HDF4/5 file. */
    public static FileFilter getFileFilter() {
        boolean extensionNotChanged = (fileExtension
                .equalsIgnoreCase(ViewProperties.getFileExtension()));

        if ((FILE_FILTER_HDF != null) && extensionNotChanged) {
            return FILE_FILTER_HDF;
        }

        // update extensions
        fileExtension = ViewProperties.getFileExtension();

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.setDescription("HDF & more");

        filter.addExtension(fileExtension);

        return (FILE_FILTER_HDF = filter);
    }

    /** @return a file filter for HDF4 file. */
    public static FileFilter getFileFilterHDF4() {
        if (FILE_FILTER_HDF4 != null) {
            return FILE_FILTER_HDF4;
        }

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("hdf");
        filter.addExtension("h4");
        filter.addExtension("hdf4");
        filter.setDescription("HDF4 files");
        FILE_FILTER_HDF4 = filter;

        return FILE_FILTER_HDF4;
    }

    /** @return a file filter for HDF5 file. */
    public static FileFilter getFileFilterHDF5() {
        if (FILE_FILTER_HDF5 != null) {
            return FILE_FILTER_HDF5;
        }

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("h5");
        filter.addExtension("hdf5");
        filter.setDescription("HDF5 files");
        FILE_FILTER_HDF5 = filter;

        return FILE_FILTER_HDF5;
    }

    /** @return a file filter for JPEG image files. */
    public static FileFilter getFileFilterJPEG() {
        if (FILE_FILTER_JPEG != null) {
            return FILE_FILTER_JPEG;
        }

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("jpg");
        filter.addExtension("jpeg");
        filter.addExtension("jpe");
        filter.addExtension("jif");
        filter.addExtension("jfif");
        filter.addExtension("jfi");
        filter.setDescription("JPEG images");
        FILE_FILTER_JPEG = filter;

        return FILE_FILTER_JPEG;
    }

    /** @return a file filter for TIFF image files. */
    public static FileFilter getFileFilterTIFF() {
        if (FILE_FILTER_TIFF != null) {
            return FILE_FILTER_TIFF;
        }

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("tif");
        filter.addExtension("tiff");
        filter.setDescription("TIFF images");
        FILE_FILTER_TIFF = filter;

        return FILE_FILTER_TIFF;
    }

    /** @return a file filter for PNG image files. */
    public static FileFilter getFileFilterPNG() {
        if (FILE_FILTER_PNG != null) {
            return FILE_FILTER_PNG;
        }

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("png");
        filter.setDescription("PNG images");
        FILE_FILTER_PNG = filter;

        return FILE_FILTER_PNG;
    }

    /** @return a file filter for BMP image files. */
    public static FileFilter getFileFilterBMP() {
        if (FILE_FILTER_BMP != null) {
            return FILE_FILTER_BMP;
        }

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("bmp");
        filter.addExtension("dib");
        filter.setDescription("BMP images");
        FILE_FILTER_BMP = filter;

        return FILE_FILTER_BMP;
    }

    /** @return a file filter for GIF image files. */
    public static FileFilter getFileFilterGIF() {
        if (FILE_FILTER_GIF != null) {
            return FILE_FILTER_GIF;
        }

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("gif");
        filter.setDescription("GIF images");
        FILE_FILTER_GIF = filter;

        return FILE_FILTER_GIF;
    }

    /** @return a file filter for GIF, JPEG, BMP, or PNG image files. */
    public static FileFilter getImageFileFilter() {
        if (FILE_FILTER_IMG != null) {
            return FILE_FILTER_IMG;
        }

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("jpg");
        filter.addExtension("jpeg");
        filter.addExtension("jpe");
        filter.addExtension("jif");
        filter.addExtension("jfif");
        filter.addExtension("jfi");
        filter.addExtension("png");
        filter.addExtension("gif");
        filter.addExtension("bmp");
        filter.addExtension("dib");
        filter.setDescription("GIF, JPEG, BMP, or PNG images");
        FILE_FILTER_IMG = filter;

        return FILE_FILTER_IMG;
    }

    /** @return a file filter for text file. */
    public static FileFilter getFileFilterText() {
        if (FILE_FILTER_TEXT != null) {
            return FILE_FILTER_TEXT;
        }

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("txt");
        filter.addExtension("text");
        filter.setDescription("Text");
        FILE_FILTER_TEXT = filter;

        return FILE_FILTER_TEXT;
    }

    /** @return a file filter for binary file. */
    public static FileFilter getFileFilterBinary() {
        if (FILE_FILTER_BINARY != null) {
            return FILE_FILTER_BINARY;
        }

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("bin");
        filter.setDescription("Binary");
        FILE_FILTER_BINARY = filter;

        return FILE_FILTER_BINARY;
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
}
