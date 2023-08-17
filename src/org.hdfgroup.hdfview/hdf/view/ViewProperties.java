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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.graphics.Image;

import hdf.HDFVersions;
import hdf.object.FileFormat;
import hdf.view.ImageView.ImageViewFactory;
import hdf.view.MetaDataView.MetaDataViewFactory;
import hdf.view.PaletteView.PaletteViewFactory;
import hdf.view.TableView.TableViewFactory;
import hdf.view.TreeView.TreeViewFactory;

/** A class to maintain the list of preferences for data and display */
public class ViewProperties extends PreferenceStore
{
    private static final long   serialVersionUID     = -6411465283887959066L;

    private static final Logger log = LoggerFactory.getLogger(ViewProperties.class);

    /** the version of the HDFViewer */
    public static final String  VERSION              = HDFVersions.getPropertyVersionView();

    /** the local property file name */
    private static final String USER_PROPERTY_FILE   = ".hdfview" + VERSION;

    /** the maximum number of most recent files */
    public static final int     MAX_RECENT_FILES     = 15;

    /** name of the tab delimiter */
    public static final String  DELIMITER_TAB        = "Tab";

    /** name of the tab delimiter */
    public static final String  DELIMITER_COMMA      = "Comma";

    /** name of the tab delimiter */
    public static final String  DELIMITER_SPACE      = "Space";

    /** name of the tab delimiter */
    public static final String  DELIMITER_COLON      = "Colon";

    /** image origin: UpperLeft */
    public static final String  ORIGIN_UL            = "UpperLeft";

    /** image origin: LowerLeft */
    public static final String  ORIGIN_LL            = "LowerLeft";

    /** image origin: UpperRight */
    public static final String  ORIGIN_UR            = "UpperRight";

    /** image origin: LowerRight */
    public static final String  ORIGIN_LR            = "LowerRight";

    /** name of the tab delimiter */
    public static final String  DELIMITER_SEMI_COLON = "Semi-Colon";

    /**
     * The names of the various default classes for each HDFView module interface
     */

    /** Text for default selection of modules */
    public static final String DEFAULT_MODULE_TEXT = "Default";

    /** Default TreeView class names */
    public static final String DEFAULT_TREEVIEW_NAME = "hdf.view.TreeView.DefaultTreeView";

    /** Default Scalar TableView class names */
    public static final String DEFAULT_SCALAR_DATASET_TABLEVIEW_NAME = "hdf.view.TableView.DefaultScalarDSTableView";
    /** Default Compound TableView class names */
    public static final String DEFAULT_COMPOUND_DATASET_TABLEVIEW_NAME = "hdf.view.TableView.DefaultCompoundDSTableView";

    /** Default Group MetaDataView class names */
    public static final String DEFAULT_GROUP_METADATAVIEW_NAME = "hdf.view.MetaDataView.DefaultGroupMetaDataView";
    /** Default Dataset MetaDataView class names */
    public static final String DEFAULT_DATASET_METADATAVIEW_NAME = "hdf.view.MetaDataView.DefaultDatasetMetaDataView";
    /** Default Datatype MetaDataView class names */
    public static final String DEFAULT_DATATYPE_METADATAVIEW_NAME = "hdf.view.MetaDataView.DefaultDatatypeMetaDataView";
    /** Default Link MetaDataView class names */
    public static final String DEFAULT_LINK_METADATAVIEW_NAME = "hdf.view.MetaDataView.DefaultLinkMetaDataView";

    /** Default ImageView class names */
    public static final String DEFAULT_IMAGEVIEW_NAME = "hdf.view.ImageView.DefaultImageView";

    /** Default PaletteView class names */
    public static final String DEFAULT_PALETTEVIEW_NAME = "hdf.view.PaletteView.DefaultPaletteView";

    /**
     * Used to create different DataViews for a given HObject.
     */
    public static enum DataViewType {
        /** */
        TABLE,
        /** */
        IMAGE,
        /** */
        PALETTE,
        /** */
        METADATA,
        /** */
        TREEVIEW
    }

    /**
     * Property keys control how the data is displayed.
     */
    public static enum DATA_VIEW_KEY {
        /** */
        CHAR,
        /** */
        CONVERTBYTE,
        /** */
        TRANSPOSED,
        /** */
        READONLY,
        /** */
        OBJECT,
        /** */
        BITMASK,
        /** */
        BITMASKOP,
        /** */
        BORDER,
        /** */
        INFO,
        /** */
        INDEXBASE1,
        /** */
        VIEW_NAME
    }

    /**
     * Property keys control how the data is displayed.
     */
    public static enum BITMASK_OP {
        /** */
        AND,
        /** */
        EXTRACT
    }

    /** the root directory of the HDFView */
    private static String            rootDir                = System.getProperty("user.dir");

    /** user's guide */
    private static String            usersGuide             = "/share/doc/UsersGuide/index.html";

    /** the font size */
    private static int               fontSize               = 12;

    /** the font type */
    private static String            fontType               = "Serif";

    /** the full path of H4toH5 converter */
    private static String            h4toh5                 = "";

    /** data delimiter */
    private static String            delimiter              = DELIMITER_TAB;

    /** image origin */
    private static String            origin                 = ORIGIN_UL;

    /** default index type */
    private static String            indexType              = "H5_INDEX_NAME";

    /** default index order */
    private static String            indexOrder             = "H5_ITER_INC";

    /** a list of most recent files */
    private static ArrayList<String> recentFiles            = new ArrayList<>(MAX_RECENT_FILES + 5);

    /** default starting file directory */
    private static String            workDir                = System.getProperty("user.dir");

    /** default HDF file extensions */
    private static String            fileExt                = "hdf, h4, hdf4, h5, hdf5, he2, he5";

    private static ClassLoader       extClassLoader         = null;

    /** a list of srb accounts */
    private static ArrayList<String[]> srbAccountList       = new ArrayList<>(5);

    /** the timer refreshrate in msec */
    private static int               timerRefresh           = 10000;

    private static boolean           isMac                  = System.getProperty("os.name").toLowerCase().contains("mac");

    /**
     * flag to indicate if auto contrast is used in image processing. Do not use
     * autocontrast by default (2.6 change).
     */
    private static boolean           isAutoContrast         = false;

    private static boolean           showImageValues        = false;

    private static boolean           showRegRefValues       = false;

    /**
     * flag to indicate if default open file mode is read only. By default, use read
     * only to prevent accidental modifications to the file.
     */
    private static boolean           isReadOnly             = true;

    /**
     * flag to indicate if default open file mode is read SWMR.
     */
    private static boolean           isReadSWMR             = true;

    private static String            EarlyLib               = "Latest";

    private static String            LateLib                = "Latest";

    /** a list of palette files */
    private static ArrayList<String> paletteList            = new ArrayList<>(5);

    /** flag to indicate if enum data is converted to strings */
    private static boolean           convertEnum            = true;

    /** flag to indicate if data is 1-based index */
    private static boolean           isIndexBase1           = false;

    /**
     * Current Java applications such as HDFView cannot handle files with a large
     * number of objects such as 1,000,000 objects. max_members defines the maximum
     * number of objects that will be loaded into memory.
     */
    private static int               maxMembers            = Integer.MAX_VALUE;   // load all by default
    /**
     * Current Java applications such as HDFView cannot handle files with a large
     * number of objects such 1,000,000 objects. start_members defines the
     * starting index of objects that will be loaded into memory.
     */
    private static int               startMembers          = 0;

    private static Image
    hdfviewIcon,
    h4Icon, h4IconR, h5Icon, h5IconR, ncIcon, ncIconR,
    blankIcon, helpIcon, fileopenIcon, filesaveIcon, filenewIcon, filecloseIcon, foldercloseIcon,
    folderopenIcon, foldercloseIconA, folderopenIconA, datasetIcon, imageIcon, tableIcon, textIcon, datasetIconA,
    imageIconA, tableIconA, textIconA, zoominIcon, zoomoutIcon, paletteIcon, chartIcon, brightIcon, autocontrastIcon,
    copyIcon, cutIcon, pasteIcon, previousIcon, nextIcon, firstIcon, lastIcon, animationIcon, datatypeIcon,
    datatypeIconA, linkIcon, iconAPPS, iconURL, iconVIDEO, iconXLS, iconPDF, iconAUDIO, questionIcon;

    private static Image[] hdfIcons = new Image[6];
    
    private static String            propertyFile;

    /** a list of treeview modules */
    private static ArrayList<String> moduleListTreeView = new ArrayList<>(5);

    /** a list of metaview modules */
    private static ArrayList<String> moduleListMetaDataView = new ArrayList<>(5);

    /** a list of tableview modules */
    private static ArrayList<String> moduleListTableView = new ArrayList<>(5);

    /** a list of imageview modules */
    private static ArrayList<String> moduleListImageView = new ArrayList<>(5);

    /** a list of paletteview modules */
    private static ArrayList<String> moduleListPaletteView = new ArrayList<>(5);

    /** a list of helpview modules */
    private static ArrayList<String> moduleListHelpView = new ArrayList<>(5);

    /**
     * Creates a property list with given root directory of the HDFView.
     *
     * @param viewRoot
     *            the root directory of the HDFView
     * @param viewStart
     *            the starting directory for file searches
     */
    public ViewProperties(String viewRoot, String viewStart) {
        super();

        // look for the property file in the user's home directory
        String propertyFileName = USER_PROPERTY_FILE;
        String userHomeFile = System.getProperty("user.home") + File.separator + propertyFileName;
        String userDirFile = System.getProperty("user.dir") + File.separator + propertyFileName;

        setFilename(createPropertyFile(userHomeFile, userDirFile));

        setRootDir(viewRoot);
        log.trace("rootDir is {}", rootDir);
        if (viewStart != null)
            setWorkDir(viewStart);
        setDefault("work.dir", getWorkDir());

        setUsersGuide(rootDir + usersGuide);

        setDefault("users.guide", viewRoot + "/UsersGuide/index.html");
        setDefault("image.contrast", false);
        setDefault("image.showvalues", false);
        setDefault("file.mode", "r");
        setDefault("lib.lowversion", "Earliest");
        setDefault("lib.highversion", "Latest");
        setDefault("enum.conversion", false);
        setDefault("regref.showvalues", false);
        setDefault("index.base1", false);
        setDefault("image.origin", ORIGIN_UL);
        setDefault("h5file.indexType", "H5_INDEX_NAME");
        setDefault("h5file.indexOrder", "H5_ITER_INC");
        setDefault("h4toh5.converter", "");
        setDefault("file.extension", "hdf, h4, hdf4, h5, hdf5, he2, he5");
        setDefault("timer.refresh", 1000);
        setDefault("font.size", 12);
        setDefault("font.type", "Serif");
        setDefault("max.members", Integer.MAX_VALUE);
        setDefault("recent.file", "");
        setDefault("palette.file", "");
        setDefault("data.delimiter", DELIMITER_TAB);
    }

    /**
     * Creates a property list file in a directory.
     *
     * @param userHomeFile
     *            the user home directory
     * @param userDirFile
     *            the user directory
     *
     * @return property list file
     */
    public static String createPropertyFile(String userHomeFile, String userDirFile) {
        String propFile = System.getProperty("hdfview.propfile");

        if ((propFile != null) && ((new File(propFile)).exists()))
            propertyFile = propFile;
        else if ((new File(userHomeFile)).exists())
            propertyFile = userHomeFile;
        else if ((new File(userDirFile)).exists())
            propertyFile = userDirFile;
        else {
            File pFile = null;

            // If the user specified a property file, but it didn't exist,
            // try to create a new one where specified.
            if (propFile != null) {
                pFile = new File(propFile);

                try {
                    pFile.createNewFile();
                    propertyFile = propFile;
                }
                catch (Exception ex) {
                    log.debug("createPropertyFile(): unable to create property file {}", propFile);
                    pFile = null;
                }
            }

            if (pFile == null) {
                // Create new property file at user home directory
                pFile = new File(userHomeFile);
                try {
                    pFile.createNewFile();
                    propertyFile = userHomeFile;
                }
                catch (Exception ex) {
                    log.debug("createPropertyFile(): unable to create property file in home directory");
                    propertyFile = null;
                }
            }
        }

        log.trace("propertyFile is {}", propertyFile);
        return propertyFile;
    }

    /**
     * load module classes
     *
     * @return the ClassLoader
     */
    public static ClassLoader loadExtClass() {
        if (extClassLoader != null)
            return extClassLoader;
        else
            // default classloader
            extClassLoader = ClassLoader.getSystemClassLoader();
        log.trace("loadExtClass: default classloader is {}", extClassLoader);

        String rootPath = System.getProperty("hdfview.root");
        if (rootPath == null) {
            rootPath = rootDir;
            log.debug("loadExtClass: rootDir rootPath is {}", rootPath);
        }
        log.debug("loadExtClass: rootPath is {}", rootPath);

        String dirname = rootPath + File.separator + "lib" + File.separator + "ext" + File.separator;
        String[] jars = null;
        File extdir = null;
        try {
            extdir = new File(dirname);
            jars = extdir.list();
        }
        catch (Exception ex0) {
            log.debug("loadExtClass: load dirname: {}+lib/ext failed", rootPath, ex0);
        }

        if ((jars == null) || (jars.length <= 0))
            return extClassLoader;

        ArrayList<String> jarList = new ArrayList<>(50);
        ArrayList<String> classList = new ArrayList<>(50);
        for (int i = 0; i < jars.length; i++) {
            log.trace("loadExtClass: load jar[{}]", i);
            if (jars[i].endsWith(".jar")) {
                jarList.add(jars[i]);
                // add class names to the list of classes
                File tmpFile = new File(extdir, jars[i]);
                try (JarFile jarFile = new JarFile(tmpFile, false, JarFile.OPEN_READ)) {
                    Enumeration<?> emu = jarFile.entries();
                    while (emu.hasMoreElements()) {
                        JarEntry jarEntry = (JarEntry) emu.nextElement();
                        String entryName = jarEntry.getName();
                        log.trace("loadExtClass: reading jar[{}] class={}", i, entryName);
                        int idx = entryName.indexOf(".class");
                        if ((idx > 0) && (entryName.indexOf('$') <= 0)) {
                            entryName = entryName.replace('/', '.');
                            classList.add(entryName.substring(0, idx));
                        }
                    }
                }
                catch (Exception ex) {
                    log.debug("loadExtClass: load jar[{}] failed", i, ex);
                }
            } // (jars[i].endsWith(".jar"))
        } // (int i=0; i<jars.length; i++)

        int n = jarList.size();
        if (n <= 0) {
            log.debug("loadExtClass: jarList empty");
            return extClassLoader;
        }

        URL[] urls = new URL[n];
        for (int i = 0; i < n; i++) {
            try {
                urls[i] = new URL("file:///" + rootPath + "/lib/ext/" + jarList.get(i));
                log.trace("loadExtClass: load urls[{}] is {}", i, urls[i]);
            }
            catch (MalformedURLException mfu) {
                log.debug("loadExtClass: load urls[{}] failed", i, mfu);
            }
        }

        try {
            extClassLoader = URLClassLoader.newInstance(urls);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        // load user modules into their list
        n = classList.size();
        for (int i = 0; i < n; i++) {
            String theName = classList.get(i);
            log.trace("loadExtClass: load classList[{}] is {}", i, theName);
            try {
                // enables use of JHDF5 in JNLP (Web Start) applications, the
                // system class loader with reflection first.
                Class<?> theClass = null;
                try {
                    theClass = Class.forName(theName);
                }
                catch (Exception ex) {
                    try {
                        theClass = extClassLoader.loadClass(theName);
                    }
                    catch (Exception exc) {
                        log.debug("load: loadClass({}) failed", theName, ex);
                    }
                }

                if(theClass != null) {
                    if (TableViewFactory.class.isAssignableFrom(theClass)) {
                        if (!moduleListTableView.contains(theName))
                            moduleListTableView.add(theName);
                        log.trace("loadExtClass: TableViewFactory class {}", theName);
                    }
                    else if (MetaDataViewFactory.class.isAssignableFrom(theClass)) {
                        if (!moduleListMetaDataView.contains(theName))
                            moduleListMetaDataView.add(theName);
                        log.trace("loadExtClass: MetaDataViewFactory class {}", theName);
                    }
                    else if (ImageViewFactory.class.isAssignableFrom(theClass)) {
                        if (!moduleListImageView.contains(theName))
                            moduleListImageView.add(theName);
                        log.trace("loadExtClass: ImageViewFactory class {}", theName);
                    }
                    else if (TreeViewFactory.class.isAssignableFrom(theClass)) {
                        if (!moduleListTreeView.contains(theName))
                            moduleListTreeView.add(theName);
                        log.trace("loadExtClass: TreeViewFactory class {}", theName);
                    }
                    else if (PaletteViewFactory.class.isAssignableFrom(theClass)) {
                        if (!moduleListPaletteView.contains(theName))
                            moduleListPaletteView.add(theName);
                        log.trace("loadExtClass: PaletteViewFactory class {}", theName);
                    }
                }
            }
            catch (Exception ex) {
                log.debug("loadExtClass: load classList[{}] of {} failed", i, theName, ex);
            }
        } //  (int i=0; i<n; i++)

        return extClassLoader;
    }

    /** @return the Folder Close Icon */
    public static Image getFoldercloseIcon() {
        return foldercloseIcon;
    }

    /** @return the Folder Close with Attribute Icon */
    public static Image getFoldercloseIconA() {
        return foldercloseIconA;
    }

    /** @return the Folder Open Icon */
    public static Image getFolderopenIcon() {
        return folderopenIcon;
    }

    /** @return the Folder Open with Attribute Icon */
    public static Image getFolderopenIconA() {
        return folderopenIconA;
    }

    /** @return the HDF Icon */
    public static Image getHdfIcon() {
        return hdfIcons[1];
    }

    /** @return the HDF Icons */
    public static Image[] getHdfIcons() {
        return hdfIcons;
    }

    /** @return the HDF4 Icon */
    public static Image getH4Icon() {
        return h4Icon;
    }

    /** @return the read-only HDF4 Icon */
    public static Image getH4IconR() {
        return h4IconR;
    }

    /** @return the HDF5 Icon */
    public static Image getH5Icon() {
        return h5Icon;
    }

    /** @return the read-only HDF5 Icon */
    public static Image getH5IconR() {
        return h5IconR;
    }

    /** @return the netcdf Icon */
    public static Image getNC3Icon() {
        return ncIcon;
    }

    /** @return the read-only netcdf Icon */
    public static Image getNC3IconR() {
        return ncIconR;
    }

    /** @return the Dataset Icon */
    public static Image getDatasetIcon() {
        return datasetIcon;
    }

    /** @return the Dataset with Attribute Icon */
    public static Image getDatasetIconA() {
        return datasetIconA;
    }

    /** @return the Datatype Icon */
    public static Image getDatatypeIcon() {
        return datatypeIcon;
    }

    /** @return the Datatype with Attribute Icon */
    public static Image getDatatypeIconA() {
        return datatypeIconA;
    }

    /** @return the Link Icon */
    public static Image getLinkIcon() {
        return linkIcon;
    }

    /** @return the File Open Icon */
    public static Image getFileopenIcon() {
        return fileopenIcon;
    }

    /** @return the File Save Icon */
    public static Image getFilesaveIcon() {
        return filesaveIcon;
    }

    /** @return the File New Icon */
    public static Image getFilenewIcon() {
        return filenewIcon;
    }

    /** @return the File Close Icon */
    public static Image getFilecloseIcon() {
        return filecloseIcon;
    }

    /** @return the Palette Icon */
    public static Image getPaletteIcon() {
        return paletteIcon;
    }

    /** @return the Bright Icon */
    public static Image getBrightIcon() {
        return brightIcon;
    }

    /** @return the Autocontrast Icon */
    public static Image getAutocontrastIcon() {
        return autocontrastIcon;
    }

    /** @return the Image Icon */
    public static Image getImageIcon() {
        return imageIcon;
    }

    /** @return the Table Icon */
    public static Image getTableIcon() {
        return tableIcon;
    }

    /** @return the Text Icon */
    public static Image getTextIcon() {
        return textIcon;
    }

    /** @return the Image with Attribute Icon */
    public static Image getImageIconA() {
        return imageIconA;
    }

    /** @return the Table with Attribute Icon */
    public static Image getTableIconA() {
        return tableIconA;
    }

    /** @return the Text with Attribute Icon */
    public static Image getTextIconA() {
        return textIconA;
    }

    /** @return the Zoom In Icon */
    public static Image getZoominIcon() {
        return zoominIcon;
    }

    /** @return the Zoom Out Icon */
    public static Image getZoomoutIcon() {
        return zoomoutIcon;
    }

    /** @return the Blank Icon */
    public static Image getBlankIcon() {
        return blankIcon;
    }

    /** @return the Help Icon */
    public static Image getHelpIcon() {
        return helpIcon;
    }

    /** @return the Copy Icon */
    public static Image getCopyIcon() {
        return copyIcon;
    }

    /** @return the Cut Icon */
    public static Image getCutIcon() {
        return cutIcon;
    }

    /** @return the Paste Icon */
    public static Image getPasteIcon() {
        return pasteIcon;
    }

    /** @return the HDFView Icon */
    public static Image getHDFViewIcon() {
        return hdfviewIcon;
    }

    /** @return the Large HDF Icon */
    public static Image getLargeHdfIcon() {
        return hdfIcons[2];
    }

    /** @return the Previous Icon */
    public static Image getPreviousIcon() {
        return previousIcon;
    }

    /** @return the Next Icon */
    public static Image getNextIcon() {
        return nextIcon;
    }

    /** @return the First Icon */
    public static Image getFirstIcon() {
        return firstIcon;
    }

    /** @return the Last Icon */
   public static Image getLastIcon() {
        return lastIcon;
    }

    /** @return the Chart Icon */
    public static Image getChartIcon() {
        return chartIcon;
    }

    /** @return the Animation Icon */
    public static Image getAnimationIcon() {
        return animationIcon;
    }

    /** @return the Apps Icon */
    public static Image getAppsIcon() {
        return iconAPPS;
    }

    /** @return the Url Icon */
    public static Image getUrlIcon() {
        return iconURL;
    }

    /** @return the Video Icon */
    public static Image getVideoIcon() {
        return iconVIDEO;
    }

    /** @return the Xls Icon */
    public static Image getXlsIcon() {
        return iconXLS;
    }

    /** @return the Pdf Icon */
    public static Image getPdfIcon() {
        return iconPDF;
    }

    /** @return the Audio Icon */
    public static Image getAudioIcon() {
        return iconAUDIO;
    }

    /** @return the Question Icon */
    public static Image getQuestionIcon() {
        return questionIcon;
    }

    /** Load the Icons */
    public static void loadIcons() {
        InputStream s = null;
        // load icon images
        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdfview.gif");
            hdfviewIcon = new Image(null, s);
        }
        catch (Exception ex) {
            hdfviewIcon = null;
            log.trace("hdfviewIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdfview16.png");
            hdfIcons[0] = new Image(null, s);            
        }
        catch (Exception ex) {
            hdfIcons[0] = null;
            log.trace("hdfIcons[0]: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdfview32.png");
            hdfIcons[1] = new Image(null, s);
        }
        catch (Exception ex) {
            hdfIcons[1] = null;
            log.trace("hdfIcons[1]: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdfview64.png");
            hdfIcons[2] = new Image(null, s);
        }
        catch (Exception ex) {
            hdfIcons[2] = null;
            log.trace("hdfIcons[3]: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdfview128.png");
            hdfIcons[3] = new Image(null, s);
        }
        catch (Exception ex) {
            hdfIcons[3] = null;
            log.trace("hdfIcons[3]: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdfview512.png");
            hdfIcons[4] = new Image(null, s);
        }
        catch (Exception ex) {
            hdfIcons[4] = null;
            log.trace("hdfIcons[4]: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdfview1024.png");
            hdfIcons[5] = new Image(null, s);
        }
        catch (Exception ex) {
            hdfIcons[5] = null;
            log.trace("hdfIcons[5]: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdf4.gif");
            h4Icon = new Image(null, s);
        }
        catch (Exception ex) {
            h4Icon = null;
            log.trace("h4Icon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdf4R.gif");
            h4IconR = new Image(null, s);
        }
        catch (Exception ex) {
            h4IconR = null;
            log.trace("h4IconR: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdf5.gif");
            h5Icon = new Image(null, s);
        }
        catch (Exception ex) {
            h5Icon = null;
            log.trace("h5Icon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdf5R.gif");
            h5IconR = new Image(null, s);
        }
        catch (Exception ex) {
            h5IconR = null;
            log.trace("h5IconR: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdfnc.gif");
            ncIcon = new Image(null, s);
        }
        catch (Exception ex) {
            ncIcon = null;
            log.trace("ncIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/hdfncR.gif");
            ncIconR = new Image(null, s);
        }
        catch (Exception ex) {
            ncIconR = null;
            log.trace("ncIconR: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/folderclose.gif");
            foldercloseIcon = new Image(null, s);
        }
        catch (Exception ex) {
            foldercloseIcon = null;
            log.trace("foldercloseIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/foldercloseA.gif");
            foldercloseIconA = new Image(null, s);
        }
        catch (Exception ex) {
            foldercloseIconA = null;
            log.trace("foldercloseIconA: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/folderopen.gif");
            folderopenIcon = new Image(null, s);
        }
        catch (Exception ex) {
            folderopenIcon = null;
            log.trace("folderopenIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/folderopenA.gif");
            folderopenIconA = new Image(null, s);
        }
        catch (Exception ex) {
            folderopenIconA = null;
            log.trace("folderopenIconA: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/dataset.gif");
            datasetIcon = new Image(null, s);
        }
        catch (Exception ex) {
            datasetIcon = null;
            log.trace("datasetIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/datasetA.gif");
            datasetIconA = new Image(null, s);
        }
        catch (Exception ex) {
            datasetIconA = null;
            log.trace("datasetIconA: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/datatype.gif");
            datatypeIcon = new Image(null, s);
        }
        catch (Exception ex) {
            datatypeIcon = null;
            log.trace("datatypeIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/datatypeA.gif");
            datatypeIconA = new Image(null, s);
        }
        catch (Exception ex) {
            datatypeIconA = null;
            log.trace("datatypeIconA: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/link.gif");
            linkIcon = new Image(null, s);
        }
        catch (Exception ex) {
            linkIcon = null;
            log.trace("linkIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/fileopen.gif");
            fileopenIcon = new Image(null, s);
        }
        catch (Exception ex) {
            fileopenIcon = null;
            log.trace("fileopenIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/filesave.gif");
            filesaveIcon = new Image(null, s);
        }
        catch (Exception ex) {
            filesaveIcon = null;
            log.trace("filesaveIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/filenew.gif");
            filenewIcon = new Image(null, s);
        }
        catch (Exception ex) {
            filenewIcon = null;
            log.trace("filenewIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/fileclose.gif");
            filecloseIcon = new Image(null, s);
        }
        catch (Exception ex) {
            filecloseIcon = null;
            log.trace("filecloseIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/palette.gif");
            paletteIcon = new Image(null, s);
        }
        catch (Exception ex) {
            paletteIcon = null;
            log.trace("paletteIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/brightness.gif");
            brightIcon = new Image(null, s);
        }
        catch (Exception ex) {
            brightIcon = null;
            log.trace("brightIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/autocontrast.gif");
            autocontrastIcon = new Image(null, s);
        }
        catch (Exception ex) {
            autocontrastIcon = null;
            log.trace("autocontrastIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/image.gif");
            imageIcon = new Image(null, s);
        }
        catch (Exception ex) {
            imageIcon = null;
            log.trace("imageIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/imageA.gif");
            imageIconA = new Image(null, s);
        }
        catch (Exception ex) {
            imageIconA = null;
            log.trace("imageIconA: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/table.gif");
            tableIcon = new Image(null, s);
        }
        catch (Exception ex) {
            tableIcon = null;
            log.trace("tableIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/tableA.gif");
            tableIconA = new Image(null, s);
        }
        catch (Exception ex) {
            tableIconA = null;
            log.trace("tableIconA: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/text.gif");
            textIcon = new Image(null, s);
        }
        catch (Exception ex) {
            textIcon = null;
            log.trace("textIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/textA.gif");
            textIconA = new Image(null, s);
        }
        catch (Exception ex) {
            textIconA = null;
            log.trace("textIconA: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/zoomin.gif");
            zoominIcon = new Image(null, s);
        }
        catch (Exception ex) {
            zoominIcon = null;
            log.trace("iconAUzoominIconDIO: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/zoomout.gif");
            zoomoutIcon = new Image(null, s);
        }
        catch (Exception ex) {
            zoomoutIcon = null;
            log.trace("zoomoutIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/blank.gif");
            blankIcon = new Image(null, s);
        }
        catch (Exception ex) {
            blankIcon = null;
            log.trace("blankIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/help.gif");
            helpIcon = new Image(null, s);
        }
        catch (Exception ex) {
            helpIcon = null;
            log.trace("helpIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/copy.gif");
            copyIcon = new Image(null, s);
        }
        catch (Exception ex) {
            copyIcon = null;
            log.trace("copyIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/cut.gif");
            cutIcon = new Image(null, s);
        }
        catch (Exception ex) {
            cutIcon = null;
            log.trace("cutIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/paste.gif");
            pasteIcon = new Image(null, s);
        }
        catch (Exception ex) {
            pasteIcon = null;
            log.trace("pasteIcon: null");
        }


        try {
            s = ViewProperties.class.getResourceAsStream("icons/previous.gif");
            previousIcon = new Image(null, s);
        }
        catch (Exception ex) {
            previousIcon = null;
            log.trace("previousIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/next.gif");
            nextIcon = new Image(null, s);
        }
        catch (Exception ex) {
            nextIcon = null;
            log.trace("nextIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/first.gif");
            firstIcon = new Image(null, s);
        }
        catch (Exception ex) {
            firstIcon = null;
            log.trace("firstIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/last.gif");
            lastIcon = new Image(null, s);
        }
        catch (Exception ex) {
            lastIcon = null;
            log.trace("lastIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/chart.gif");
            chartIcon = new Image(null, s);
        }
        catch (Exception ex) {
            chartIcon = null;
            log.trace("chartIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/animation.gif");
            animationIcon = new Image(null, s);
        }
        catch (Exception ex) {
            animationIcon = null;
            log.trace("animationIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/question.gif");
            questionIcon = new Image(null, s);
        }
        catch (Exception ex) {
            questionIcon = null;
            log.trace("questionIcon: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/audio.gif");
            iconAUDIO = new Image(null, s);
        }
        catch (Exception ex) {
            iconAUDIO = null;
            log.trace("iconAUDIO: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/xls.gif");
            iconXLS = new Image(null, s);
        }
        catch (Exception ex) {
            iconXLS = null;
            log.trace("iconXLS: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/pdf.gif");
            iconPDF = new Image(null, s);
        }
        catch (Exception ex) {
            iconPDF = null;
            log.trace("iconPDF: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/apps.gif");
            iconAPPS = new Image(null, s);
        }
        catch (Exception ex) {
            iconAPPS = null;
            log.trace("iconAPPS: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/url.gif");
            iconURL = new Image(null, s);
        }
        catch (Exception ex) {
            iconURL = null;
            log.trace("iconURL: null");
        }

        try {
            s = ViewProperties.class.getResourceAsStream("icons/video.gif");
            iconVIDEO = new Image(null, s);
        }
        catch (Exception ex) {
            iconVIDEO = null;
            log.trace("iconVIDEO: null");
        }
    }

    /**
     * Load user properties from property file
     *
     * @throws IOException
     *             if a failure occurred
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void load() throws IOException {
        super.load();

        if (propertyFile == null)
            return;

        String propVal = null;

        // add default module.
        log.trace("load user properties: add default modules");
        String[] moduleKeys = { "module.treeview", "module.metadataview", "module.tableview",
                "module.imageview", "module.paletteview" };
        ArrayList[] moduleList = { moduleListTreeView, moduleListMetaDataView, moduleListTableView,
                moduleListImageView, moduleListPaletteView };
        String[] moduleNames = { DEFAULT_MODULE_TEXT, DEFAULT_MODULE_TEXT, DEFAULT_MODULE_TEXT,
                DEFAULT_MODULE_TEXT, DEFAULT_MODULE_TEXT };

        // add default implementation of modules
        log.trace("load user properties: modules");
        for (int i = 0; i < moduleNames.length; i++) {
            if (!moduleList[i].contains(moduleNames[i]))
                moduleList[i].add(moduleNames[i]);
            log.trace("load: add default moduleList[{}] is {}", i, moduleNames[i]);
        }
        log.trace("load Ext Class modules");
        if (extClassLoader == null) loadExtClass();

        // set default selection of data views
        log.trace("load user properties: set default selection of data views");
        for (int i = 0; i < moduleNames.length; i++) {
            ArrayList<String> theList = moduleList[i];
            propVal = getString(moduleKeys[i]);
            if (log.isTraceEnabled()) {
                log.trace("load: default theList is {}", Arrays.toString(theList.toArray()));
            }

            if ((propVal != null) && (propVal.length() > 0)) {
                // set default to the module specified in property file
                if (theList.size() > 1) {
                    if (theList.contains(propVal))
                        theList.remove(propVal);
                    theList.add(0, propVal);
                }
                log.trace("load user properties: module[{}]={}", i, propVal);
            }
            else {
                // use default module
                if (theList.size() > 1) {
                    if (theList.contains(moduleNames[i]))
                        theList.remove(moduleNames[i]);
                    theList.add(0, moduleNames[i]);
                }
                log.trace("load user properties: default module[{}]={}", i, moduleNames[i]);
            }
            if (log.isTraceEnabled()) {
                log.trace("load: final theList is {}", Arrays.toString(theList.toArray()));
            }
        }

        // add fileformat modules
        log.trace("load user properties: fileformat modules");
        String[] localEnum = this.preferenceNames();
        String fExt = null;
        for (String theKey : localEnum) {
            log.trace("load: add prop {}", theKey);
            if (theKey.startsWith("module.fileformat")) {
                fExt = theKey.substring(18);
                try {
                    // enables use of JHDF5 in JNLP (Web Start) applications,
                    // the system class loader with reflection first.
                    String className = getString(theKey);
                    Class theClass = null;
                    try {
                        theClass = Class.forName(className);
                    }
                    catch (Exception ex) {
                        try {
                            theClass = extClassLoader.loadClass(className);
                        }
                        catch (Exception ex2) {
                            log.debug("load: extClassLoader.loadClass({}) failed", className, ex2);
                        }
                    }

                    Object theObject = theClass.newInstance();
                    if (theObject instanceof FileFormat) {
                        FileFormat.addFileFormat(fExt, (FileFormat) theObject);
                    }
                }
                catch (Exception err) {
                    log.debug("load: load file format failed", err);
                }
            }
        }

        propVal = getString("users.guide");
        if (!isDefault("users.guide"))
            setUsersGuide(propVal);

        propVal = getString("image.contrast");
        if (!isDefault("image.contrast"))
            setAutoContrast("auto".equalsIgnoreCase(propVal));

        setShowImageValue(getBoolean("image.showvalues"));

        propVal = getString("file.mode");
        if (!isDefault("file.mode"))
            setReadOnly("r".equalsIgnoreCase(propVal));

        setEarlyLib(getString("lib.lowversion"));

        setLateLib(getString("lib.highversion"));

        setConvertEnum(getBoolean("enum.conversion"));

        setShowRegRefValue(getBoolean("regref.showvalues"));

        setIndexBase1(getBoolean("index.base1"));

        propVal = getString("data.delimiter");
        if (!isDefault("data.delimiter"))
            setDataDelimiter(propVal);

        propVal = getString("image.origin");
        if (!isDefault("image.origin"))
            setImageOrigin(propVal);

        propVal = getString("h5file.indexType");
        if (!isDefault("h5file.indexType"))
            setIndexType(propVal);

        propVal = getString("h5file.indexOrder");
        if (!isDefault("h5file.indexOrder"))
            setIndexOrder(propVal);

        propVal = getString("h4toh5.converter");
        if (!isDefault("h4toh5.converter"))
            setH4toH5(propVal);

        propVal = getString("work.dir");
        if (!isDefault("work.dir"))
            setWorkDir(propVal);

        propVal = getString("file.extension");
        if (!isDefault("file.extension")) {
            setFileExtension(propVal);
            FileFormat.addFileExtension(fileExt);
        }

        setTimerRefresh(getInt("timer.refresh"));

        setFontSize(getInt("font.size"));

        propVal = getString("font.type");
        if (!isDefault("font.type"))
            setFontType(propVal.trim());

        setMaxMembers(getInt("max.members"));

        // load the most recent file list from the property file
        log.trace("load user properties: most recent file list with {}", getWorkDir());
        String theFile = null;
        // first entry should be the working dir
        recentFiles.add(getWorkDir());
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            theFile = getString("recent.file" + i);
            if ((theFile != null) && !recentFiles.contains(theFile)) {
                if (theFile.startsWith("http://") || theFile.startsWith("ftp://") || (new File(theFile)).exists()) {
                    recentFiles.add(theFile);
                }
            }
        }

        // load the most recent palette file list from the property file
        log.trace("load user properties: most recent palette file list");
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            theFile = getString("palette.file" + i);
            if (theFile != null) theFile = theFile.trim();

            if ((theFile != null && theFile.length() > 0) && !paletteList.contains(theFile)) {
                if ((new File(theFile)).exists()) {
                    paletteList.add(theFile);
                }
            }
        }
    }

    /**
     * Save user properties into property file
     *
     * @throws IOException
     *             if a failure occurred
     */
    @Override
    public void save() throws IOException {
        if (propertyFile == null)
            return;

        // update data saving options
        if (delimiter == null)
            setDefault("data.delimiter", DELIMITER_TAB);
        else
            setValue("data.delimiter", delimiter);

        if (origin == null)
            setDefault("image.origin", ORIGIN_UL);
        else
            setValue("image.origin", origin);

        if (indexType != null) setValue("h5file.indexType", indexType);

        if (indexOrder != null) setValue("h5file.indexOrder", indexOrder);

        if (usersGuide != null) setValue("users.guide", usersGuide);

        if (workDir != null) setValue("work.dir", workDir);

        if (fileExt != null) setValue("file.extension", fileExt);

        if (h4toh5 != null) setValue("h4toh5.converter", h4toh5);

        setValue("timer.refresh", timerRefresh);

        setValue("font.size", fontSize);

        if (fontType != null) setValue("font.type", fontType);

        setValue("max.members", maxMembers);

        if (isAutoContrast)
            setValue("image.contrast", "auto");
        else
            setValue("image.contrast", "general");

        setValue("image.showvalues", showImageValues);

        if (isReadOnly)
            setValue("file.mode", "r");
        else if (isReadSWMR)
            setValue("file.mode", "rs");
        else
            setValue("file.mode", "rw");

        log.trace("save user properties: lib.lowversion={}", EarlyLib);
        setValue("lib.lowversion", EarlyLib);
        log.trace("save user properties: lib.highversion={}", LateLib);
        setValue("lib.highversion", LateLib);

        setValue("enum.conversion", convertEnum);
        setValue("regref.showvalues", showRegRefValues);
        setValue("index.base1", isIndexBase1);

        // save the list of most recent files
        log.trace("save user properties: most recent files");
        String theFile;
        int size = recentFiles.size();
        int minSize = Math.min(size, MAX_RECENT_FILES);
        log.trace("save user properties: most recent files size={}", size);
        // The first entry is always the working dir
        for (int i = 0; i < minSize - 1; i++) {
            theFile = recentFiles.get(i+1);
            log.trace("save user properties: save recent file={}", theFile);
            if ((theFile != null) && (theFile.length() > 0)) setValue("recent.file" + i, theFile);
        }

        // save the list of most recent palette files
        log.trace("save user properties: most recent palette files");
        size = paletteList.size();
        minSize = Math.min(size, MAX_RECENT_FILES);
        for (int i = 0; i < minSize; i++) {
            theFile = paletteList.get(i);
            if ((theFile != null) && (theFile.length() > 0)) setValue("palette.file" + i, theFile);
        }

        // save default modules
        log.trace("save user properties: default modules");
        String moduleName = moduleListTreeView.get(0);
        if ((moduleName != null) && (moduleName.length() > 0)) setValue("module.treeview", moduleName);
        log.trace("save user properties: module.treeview={}", moduleName);

        moduleName = moduleListMetaDataView.get(0);
        if ((moduleName != null) && (moduleName.length() > 0)) setValue("module.metadataview", moduleName);
        log.trace("save user properties: module.metadataview={}", moduleName);

        moduleName = moduleListTableView.get(0);
        if ((moduleName != null) && (moduleName.length() > 0)) setValue("module.tableview", moduleName);
        log.trace("save user properties: module.tableview={}", moduleName);

        moduleName = moduleListImageView.get(0);
        if ((moduleName != null) && (moduleName.length() > 0)) setValue("module.imageview", moduleName);
        log.trace("save user properties: module.imageview={}", moduleName);

        moduleName = moduleListPaletteView.get(0);
        if ((moduleName != null) && (moduleName.length() > 0)) setValue("module.paletteview", moduleName);
        log.trace("save user properties: module.paletteview={}", moduleName);

        // save the current supported fileformat
        log.trace("save user properties: supported fileformat");
        Enumeration<?> keys = FileFormat.getFileFormatKeys();
        String theKey = null;
        while (keys.hasMoreElements()) {
            theKey = (String) keys.nextElement();
            FileFormat theformat = FileFormat.getFileFormat(theKey);
            setValue("module.fileformat." + theKey, theformat.getClass().getName());
        }

        super.save();
    }

    /** @return the name of the user property file */
    public static String getPropertyFile() {
        return propertyFile;
    }

    /** @return the root directory where the HDFView is installed. */
    public static String getViewRoot() {
        return rootDir;
    }

    /** @return the default work directory, where the open file starts. */
    public static String getWorkDir() {
        String workPath = workDir;
        log.trace("getWorkDir: workDir={}", workDir);
        if (workPath == null) {
            workPath = System.getProperty("hdfview.workdir");
            log.trace("getWorkDir: hdfview.workdir={}", workPath);
            if (workPath == null) {
                workPath = System.getProperty("user.dir");
            }
        }
        log.trace("getWorkDir: final workPath={}", workPath);
        return workPath;
    }

    /** @return the maximum number of the most recent file */
    public static int getMaxRecentFiles() {
        return MAX_RECENT_FILES;
    }

    /** @return the path of the HDFView users guide */
    public static String getUsersGuide() {
        return usersGuide;
    };

    /** @return the delimiter of data values */
    public static String getDataDelimiter() {
        return delimiter;
    }

    /** @return the image origin */
    public static String getImageOrigin() {
        return origin;
    }

    /** @return the default index type for display */
    public static String getIndexType() {
        return indexType;
    }

    /** @return the default index order for display */
    public static String getIndexOrder() {
        return indexOrder;
    }

    /** @return the timer refresh size */
    public static int getTimerRefresh() {
        return timerRefresh;
    }

    /** sets the timer refresh
     *
     * @param trefresh
     *            the timer refresh
     */
    public static void setTimerRefresh(int trefresh) {
        timerRefresh = trefresh;
    }

    /** @return the font size */
    public static int getFontSize() {
        return fontSize;
    }

    /** @return the font type */
    public static String getFontType() {
        return fontType;
    }

    /** @return the file extensions of supported file formats */
    public static String getFileExtension() {
        return fileExt;
    }

    /** sets the font size
     *
     * @param fsize
     *            the font size
     */
    public static void setFontSize(int fsize) {
        fontSize = (fsize / 2) * 2;

        if (fontSize < 8) {
            fontSize = 8;
        }
    }

    /** sets the font type
     *
     * @param ftype
     *            the font type
     */
    public static void setFontType(String ftype) {
        if (ftype != null) {
            fontType = ftype.trim();
        }
    }

    /** @return the path of the H5toH5 converter */
    public static String getH4toH5() {
        return h4toh5;
    }

    /** @return the list of most recent files */
    public static List<String> getMRF() {
        return recentFiles;
    }

    /** @return the list of palette files */
    public static List<String> getPaletteList() {
        return paletteList;
    }

    /** @return the SRB account list */
    public static List<String[]> getSrbAccount() {
        return srbAccountList;
    }

    /** @return a list of treeview modules */
    public static List<String> getTreeViewList() {
        return moduleListTreeView;
    }

    /** @return a list of metadataview modules */
    public static List<String> getMetaDataViewList() {
        return moduleListMetaDataView;
    }

    /** @return a list of tableview modules */
    public static List<String> getTableViewList() {
        return moduleListTableView;
    }

    /** @return a list of imageview modules */
    public static List<String> getImageViewList() {
        return moduleListImageView;
    }

    /** @return a list of paletteview modules */
    public static List<String> getPaletteViewList() {
        return moduleListPaletteView;
    }

    /** @return a list of helpview modules */
    public static List<String> getHelpViewList() {
        return moduleListHelpView;
    }

    /** set the path of H5View User's guide
     *
     * @param str
     *            the path
     */
    public static void setUsersGuide(String str) {
        if ((str == null) || (str.length() <= 0)) {
            return;
        }
        usersGuide = str;
    }

    /** set the path of the H4 to H5 converter
     *
     * @param tool
     *            the path of the H4 to H5 converter
     */
    public static void setH4toH5(String tool) {
        h4toh5 = tool;
    }

    /**
     * set the path of the default root directory
     *
     * @param rDir
     *            the default root directory
     */
    public static void setRootDir(String rDir) {
        log.trace("ViewProperties:setRootDir rDir={}", rDir);
        rootDir = rDir;
    }

    /** set the path of the default work directory
     *
     * @param wDir
     *            the default work directory
     */
    public static void setWorkDir(String wDir) {
        log.trace("ViewProperties:setWorkDir wDir={}", wDir);
        workDir = wDir;
    }

    /** set the file extension
     *
     * @param ext
     *            the file extension
     */
    public static void setFileExtension(String ext) {
        fileExt = ext;
    }

    /** set the delimiter of data values
     *
     * @param delim
     *            the delimiter of data values
     */
    public static void setDataDelimiter(String delim) {
        delimiter = delim;
    }

    /** set the image origin
     *
     * @param o
     *            the image origin
     */
    public static void setImageOrigin(String o) {
        origin = o;
    }

    /** set the index type
     *
     * @param idxType
     *            the index type
     */
    public static void setIndexType(String idxType) {
        indexType = idxType;
    }

    /** set the index order
     *
     * @param idxOrder
     *            the index order
     */
    public static void setIndexOrder(String idxOrder) {
        indexOrder = idxOrder;
    }

    /**
     * Current Java applications such as HDFView cannot handle files with large
     * number of objects such as 1,000,000 objects. setMaxMembers() sets the
     * maximum number of objects that will be loaded into memory.
     *
     * @param n
     *            the maximum number of objects to load into memory
     */
    public static void setMaxMembers(int n) {
        maxMembers = n;
    }

    /**
     * Current Java applications such as HDFView cannot handle files with large
     * number of objects such as 1,000,000 objects. setStartMember() sets the
     * starting index of objects that will be loaded into memory.
     *
     * @param idx
     *            the maximum number of objects to load into memory
     */
    public static void setStartMembers(int idx) {
        if (idx < 0) {
            idx = 0;
        }

        startMembers = idx;
    }

    /**
     * Current Java applications such as HDFView cannot handle files with large
     * number of objects such as 1,000,000 objects. getMaxMembers() returns the
     * maximum number of objects that will be loaded into memory.
     *
     * @return the maximum members
     */
    public static int getMaxMembers() {
        if (maxMembers < 0)
            return Integer.MAX_VALUE; // load the whole file

        return maxMembers;
    }

    /**
     * Current Java applications such as HDFView cannot handle files with large
     * number of objects such as 1,000,000 objects. getStartMembers() returns the
     * starting index of objects that will be loaded into memory.
     *
     * @return the start members
     */
    public static int getStartMembers() {
        return startMembers;
    }

    /**
     * Returns true if auto contrast is used in image processing.
     *
     * @return true if auto contrast is used in image processing; otherwise,
     *         returns false.
     */
    public static boolean isAutoContrast() {
        return isAutoContrast;
    }

    /**
     * Returns true if "show image values" is set.
     *
     * @return true if "show image values" is set; otherwise, returns false.
     */
    public static boolean showImageValues() {
        return showImageValues;
    }

    /**
     * Set the flag to indicate if auto contrast is used in image process.
     *
     * @param b
     *            the flag to indicate if auto contrast is used in image
     *            process.
     */
    public static void setAutoContrast(boolean b) {
        isAutoContrast = b;
    }

    /**
     * Set the flag to indicate if "show image values" is set.
     *
     * @param b
     *            the flag to indicate if if "show image values" is set.
     */
    public static void setShowImageValue(boolean b) {
        showImageValues = b;
    }

    /**
     * Returns true if default file access is read only.
     *
     * @return true if default file access is read only; otherwise, returns
     *         false.
     */
    public static boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * Set the flag to indicate if default file access is read only.
     *
     * @param b
     *            the flag to indicate if default file access is read only.
     */
    public static void setReadOnly(boolean b) {
        isReadOnly = b;
    }

    /**
     * Returns true if default file access is read SWMR.
     *
     * @return true if default file access is read SWMR; otherwise, returns
     *         false.
     */
    public static boolean isReadSWMR() {
        return isReadSWMR;
    }

    /**
     * Set the flag to indicate if default file access is read SWMR.
     *
     * @param b
     *            the flag to indicate if default file access is read SWMR.
     */
    public static void setReadSWMR(boolean b) {
        isReadSWMR = b;
    }

    /**
     * Returns value of default lib version for the earliest.
     *
     * @return value of default lib version for the earliest.
     */
    public static String getEarlyLib() {
        return EarlyLib;
    }

    /**
     * Set the value of default lib version for the earliest.
     *
     * @param vers
     *            the value of default lib version for the earliest.
     */
    public static void setEarlyLib(String vers) {
        EarlyLib = vers;
    }

    /**
     * Returns value of default lib version for the latest.
     *
     * @return value of default lib version for the latest.
     */
    public static String getLateLib() {
        return LateLib;
    }

    /**
     * Set the value of default lib version for the latest.
     *
     * @param vers
     *            the value of default lib version for the latest.
     */
    public static void setLateLib(String vers) {
        LateLib = vers;
    }

    /**
     * @return the convertEnum
     */
    public static boolean isConvertEnum() {
        return convertEnum;
    }

    /**
     * Returns true if "show regref values" is set.
     *
     * @return true if "show regref values" is set; otherwise, returns false.
     */
    public static boolean showRegRefValues() {
        return showRegRefValues;
    }

    /**
     * @return the isIndexBase1
     */
    public static boolean isIndexBase1() {
        return isIndexBase1;
    }

    /**
     * @param convertEnum
     *            the convertEnum to set
     */
    public static void setConvertEnum(boolean convertEnum) {
        ViewProperties.convertEnum = convertEnum;
    }

    /**
     * Set the flag to indicate if "show RegRef values" is set.
     *
     * @param b
     *            the flag to indicate if if "show RegRef values" is set.
     */
    public static void setShowRegRefValue(boolean b) {
        showRegRefValues = b;
    }

    /**
     * Set the flag to indicate if IndexBase should start at 1.
     *
     * @param b
     *            the flag to indicate if IndexBase should start at 1.
     */
    public static void setIndexBase1(boolean b) {
        ViewProperties.isIndexBase1 = b;
    }

    /**
     * Sets the list of most recently accessed files.
     *
     * @param recentFilesList
     *               The list of most recently accessed files.
     */
    public static void setRecentFiles(ArrayList<String> recentFilesList) {
        recentFiles = recentFilesList;
    }
}
