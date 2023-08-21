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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * A convenience implementation of FileFilter that filters out all files except
 * for those type extensions that it knows about.
 *
 * @author Jordan T. Henderson
 * @version 2.4 4/27/2016
 */
public class DefaultFileFilter {
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
        this.filters = new Hashtable<>();
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
     * Note that the "." before the extension is not needed and will be ignored.
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
     * @return the file extensions associated with this DefaultFileFilter
     */
    public String getExtensions() {
        Enumeration<String> extensions = filters.keys();
        String extString = "";

        while (extensions.hasMoreElements()) {
            extString += "*." + extensions.nextElement();
            if (extensions.hasMoreElements()) {
                extString += ";";
            }
        }

        return extString;
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
            filters = new Hashtable<>(5);
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
                                + "." + extensions.nextElement();
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
    public static DefaultFileFilter getFileFilter() {
        // update extensions
        String fileExtensions = ViewProperties.getFileExtension();

        DefaultFileFilter filter = new DefaultFileFilter();
        filter.setDescription("HDF & more");

        filter.addExtension(fileExtensions);

        return filter;
    }

    /** @return a file filter for NetCDF3 file. */
    public static DefaultFileFilter getFileFilterNetCDF3() {
        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("nc");
        filter.setDescription("NetCDF3 files");

        return filter;
    }

    /** @return a file filter for HDF4 file. */
    public static DefaultFileFilter getFileFilterHDF4() {
        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("hdf");
        filter.addExtension("h4");
        filter.addExtension("hdf4");
        filter.setDescription("HDF4 files");

        return filter;
    }

    /** @return a file filter for HDF5 file. */
    public static DefaultFileFilter getFileFilterHDF5() {
        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("h5");
        filter.addExtension("hdf5");
        filter.setDescription("HDF5 files");

        return filter;
    }

    /** @return a file filter for JPEG image files. */
    public static DefaultFileFilter getFileFilterJPEG() {
        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("jpg");
        filter.addExtension("jpeg");
        filter.addExtension("jpe");
        filter.addExtension("jif");
        filter.addExtension("jfif");
        filter.addExtension("jfi");
        filter.setDescription("JPEG images");

        return filter;
    }

    /** @return a file filter for TIFF image files. */
    public static DefaultFileFilter getFileFilterTIFF() {
        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("tif");
        filter.addExtension("tiff");
        filter.setDescription("TIFF images");

        return filter;
    }

    /** @return a file filter for PNG image files. */
    public static DefaultFileFilter getFileFilterPNG() {
        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("png");
        filter.setDescription("PNG images");

        return filter;
    }

    /** @return a file filter for BMP image files. */
    public static DefaultFileFilter getFileFilterBMP() {
        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("bmp");
        filter.addExtension("dib");
        filter.setDescription("BMP images");

        return filter;
    }

    /** @return a file filter for GIF image files. */
    public static DefaultFileFilter getFileFilterGIF() {
        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("gif");
        filter.setDescription("GIF images");

        return filter;
    }

    /** @return a file filter for GIF, JPEG, BMP, or PNG image files. */
    public static DefaultFileFilter getImageFileFilter() {
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

        return filter;
    }

    /** @return a file filter for text file. */
    public static DefaultFileFilter getFileFilterText() {
        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("txt");
        filter.addExtension("text");
        filter.setDescription("Text");

        return filter;
    }

    /** @return a file filter for binary file. */
    public static DefaultFileFilter getFileFilterBinary() {
        DefaultFileFilter filter = new DefaultFileFilter();
        filter.addExtension("bin");
        filter.setDescription("Binary");

        return filter;
    }
}
