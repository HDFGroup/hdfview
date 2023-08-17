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

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileFormat defines general interfaces for working with files whose data is
 * organized according to a supported format.
 *
 * FileFormat is a pluggable component. New implementing classes of FileFormat
 * can be added to the list of supported file formats. Current implementing
 * classes include H5File and H4File. By default, H5File and H4File are added to
 * the list of supported file formats maintained by the static FileFormat
 * instance.
 *
 * <pre>
 *                                    FileFormat
 *                       _________________|_________________
 *                       |                |                |
 *                     H5File          H4File           Other...
 * </pre>
 *
 * A FileFormat instance may exist without being associated with a given file. A
 * FileFormat instance may be associated with a file that is not open for
 * access. Most typically, a FileFormat instance is used to open the associated
 * file and perform operations such as retrieval and manipulation (if the file
 * access is read-write) of the file structure and objects.
 *
 * @author Peter X. Cao
 * @version 2.4 9/4/2007
 */
public abstract class FileFormat extends File {
    private static final long                    serialVersionUID   = -4700692313888420796L;

    private static final Logger log = LoggerFactory.getLogger(FileFormat.class);

    /***************************************************************************
     * File access flags used in calls to createInstance( String, flag );
     **************************************************************************/

    /**
     * File first time access flag for open file. With this access flag, added
     * to the regular value, indicates this file has no existing state.
     *
     */
    public static final int                      OPEN_NEW           = 1;

    /**
     * File access flag for read-only permission. With this access flag,
     * modifications to the file will not be allowed.
     *
     * @see #createInstance(String, int )
     */
    public static final int                      READ               = 2;

    /**
     * File access flag for read/write permission. With this access flag,
     * modifications to the file will be allowed. Behavior if the file does not
     * exist or cannot be opened for read/write access depends on the
     * implementing class.
     *
     * @see #createInstance(String, int)
     */
    public static final int                      WRITE              = 4;

    /**
     * File access flag for creating/truncating with read-write permission. If
     * the file already exists, it will be truncated when opened. With this
     * access flag, modifications to the file will be allowed. Behavior if file
     * can't be created, or if it exists but can't be opened for read/write
     * access, depends on the implementing class.
     *
     * @see #createInstance(String, int )
     */
    public static final int                      CREATE             = 8;

    /**
     * File access flag for multi-read-only permission. With this access flag,
     * modifications to the file will not be allowed.
     *
     * @see #createInstance(String, int )
     */
    public static final int                      MULTIREAD          = 80;

    /***************************************************************************
     * File creation flags used in calls to createFile( String, flag );
     **************************************************************************/

    /**
     * Flag for creating/truncating a file. If the file already exists, it will
     * be truncated when opened. If the file does not exist, it will be created.
     * Modifications to the file will be allowed.
     *
     * @see #createFile(String, int )
     */
    public static final int                      FILE_CREATE_DELETE = 10;

    /**
     * Flag for creating/opening a file. If the file already exists, it will be
     * opened without changing the existing contents. If the file does not
     * exist, it will be created. Modifications to the file will be allowed.
     *
     * @see #createFile(String, int )
     */
    public static final int                      FILE_CREATE_OPEN   = 20;

    /**
     * Flag to indicate if the earliest version of library is used when creating
     * a new file.
     *
     * @see #createFile(String, int )
     */
    public static final int                      FILE_CREATE_EARLY_LIB   = 40;


    /***************************************************************************
     * Keys and fields related to supported file formats.
     **************************************************************************/

    /** Key for HDF4 file format. */
    public static final String                   FILE_TYPE_HDF4     = "HDF4";

    /** Key for HDF5 file format. */
    public static final String                   FILE_TYPE_HDF5     = "HDF5";

    /** Key for NetCDF file format. */
    public static final String                   FILE_TYPE_NC3      = "NetCDF3";

    /**
     * A separator that separates file name and object name.
     *
     * @see hdf.object.FileFormat#getHObject(String)
     */
    public static final String                   FILE_OBJ_SEP       = "://";

    /**
     * FileList keeps a list of supported FileFormats. This list can be updated
     * and queried at runtime.
     *
     * @see #addFileFormat(String,FileFormat)
     * @see #getFileFormat(String)
     * @see #getFileFormatKeys()
     * @see #getFileFormats()
     * @see #removeFileFormat(String)
     */
    private static final Map<String, FileFormat> FileList = new Hashtable<>(10);

    /**
     * A list of file extensions for the supported file formats. This list of
     * file extensions is not integrated with the supported file formats kept in
     * FileList, but is provided as a convenience for applications who may
     * choose to process only those files with recognized extensions.
     */
    private static String extensions         = "hdf, h4, hdf5, h5, nc, fits";

    /***************************************************************************
     * Sizing information and class metadata
     **************************************************************************/

    /**
     * Current Java applications, such as HDFView, cannot handle files with
     * large numbers of objects due to JVM memory limitations. For example,
     * 1,000,000 objects is too many. max_members is defined so that
     * applications such as HDFView will load up to <i>max_members</i> objects
     * starting with the <i>start_members</i> -th object. The implementing class
     * has freedom in its interpretation of how to "count" objects in the file.
     */
    private int                                  max_members        = 10000;      // 10,000 by default
    /**
     * The start number for max_members .
     */
    private int                                  start_members      = 0;          // 0 by default

    /**
     * File identifier. -1 indicates the file is not open.
     */
    protected long                                fid                = -1;

    /**
     * The absolute pathname (path+name) of the file.
     */
    protected String                             fullFileName       = null;

    /**
     * Flag indicating if the file access is read-only.
     */
    protected boolean                            isReadOnly         = false;

    /***************************************************************************
     * Class initialization method
     **************************************************************************/

    /**
     * By default, HDF4 and HDF5 file formats are added to the supported formats
     * list.
     */
    static {
        // add HDF4 to default modules
        if (FileFormat.getFileFormat(FILE_TYPE_HDF4) == null) {
            try {
                @SuppressWarnings("rawtypes")
                Class fileclass = Class.forName("hdf.object.h4.H4File");
                FileFormat fileformat = (FileFormat) fileclass.newInstance();
                if (fileformat != null) {
                    FileFormat.addFileFormat(FILE_TYPE_HDF4, fileformat);
                    log.debug("FILE_TYPE_HDF4 file format added");
                }
            }
            catch (Exception err) {
                log.debug("FILE_TYPE_HDF4 instance failure: ", err);
            }
        }

        // add HDF5 to default modules
        if (FileFormat.getFileFormat(FILE_TYPE_HDF5) == null) {
            try {
                @SuppressWarnings("rawtypes")
                Class fileclass = Class.forName("hdf.object.h5.H5File");
                FileFormat fileformat = (FileFormat) fileclass.newInstance();
                if (fileformat != null) {
                    FileFormat.addFileFormat(FILE_TYPE_HDF5, fileformat);
                    log.debug("FILE_TYPE_HDF5 file format added");
                }
            }
            catch (Exception err) {
                log.debug("FILE_TYPE_HDF5 instance failure: ", err);
            }
        }

        // add NetCDF to default modules
        if (FileFormat.getFileFormat(FILE_TYPE_NC3) == null) {
            try {
                @SuppressWarnings("rawtypes")
                Class fileclass = Class.forName("hdf.object.nc2.NC2File");
                FileFormat fileformat = (FileFormat) fileclass.newInstance();
                if (fileformat != null) {
                    FileFormat.addFileFormat(FILE_TYPE_NC3, fileformat);
                    log.debug("NetCDF3 file format added");
                }
            }
            catch (Exception err) {
                log.debug("NetCDF3 instance failure: ", err);
            }
        }

        // add FITS to default modules
        if (FileFormat.getFileFormat("FITS") == null) {
            try {
                @SuppressWarnings("rawtypes")
                Class fileclass = Class.forName("hdf.object.fits.FitsFile");
                FileFormat fileformat = (FileFormat) fileclass.newInstance();
                if (fileformat != null) {
                    FileFormat.addFileFormat("FITS", fileformat);
                    log.debug("Fits file format added");
                }
            }
            catch (Exception err) {
                log.debug("FITS instance failure: ", err);
            }
        }

    }

    /***************************************************************************
     * Constructor
     **************************************************************************/

    /**
     * Creates a new FileFormat instance with the given filename.
     *
     * The filename in this method call is equivalent to the pathname in the
     * java.io.File class. The filename is converted into an abstract pathname
     * by the File class.
     *
     * Typically this constructor is not called directly, but is called by a
     * constructor of an implementing class. Applications most frequently use
     * the <i>createFile()</i>, <i>createInstance()</i>, or <i>getInstance()</i>
     * methods to generate a FileFormat instance with an associated filename.
     *
     * The file is not opened by this call. The read-only flag is set to false
     * by this call.
     *
     * @param filename
     *            The filename; a pathname string.
     * @throws NullPointerException
     *             If the <code>filename</code> argument is <code>null</code>.
     * @see java.io.File#File(String)
     * @see #createFile(String, int)
     * @see #createInstance(String, int)
     * @see #getInstance(String)
     */
    public FileFormat(String filename) {
        super(filename);

        fullFileName = filename;

        if ((filename != null) && (filename.length() > 0)) {
            try {
                fullFileName = this.getAbsolutePath();
            }
            catch (Exception ex) {
                log.debug("File {} getAbsolutePath failure: ", filename, ex);
            }
        }
        isReadOnly = false;
        log.trace("fullFileName={} isReadOnly={}", fullFileName, isReadOnly);
    }

    /***************************************************************************
     * Class methods
     **************************************************************************/

    /**
     * Adds a FileFormat with specified key to the list of supported formats.
     *
     * This method allows a new FileFormat, tagged with an identifying key, to
     * be added dynamically to the list of supported File Formats. Using it,
     * applications can add new File Formats at runtime.
     *
     * For example, to add a new File Format with the key "xyz" that is
     * implemented by the class xyzFile in the package companyC.files, an
     * application would make the following calls:
     *
     * <pre>
     *    Class fileClass = Class.forName( "companyC.files.xyzFile" );
     *    FileFormat ff = (FileFormat) fileClass.newInstance();
     *    if ( ff != null ) {
     *       ff.addFileFormat ("xyz", ff )
     *    }
     * </pre>
     *
     * If either <code>key</code> or <code>fileformat</code> are
     * <code>null</code>, or if <code>key</code> is already in use, the method
     * returns without updating the list of supported File Formats.
     *
     * @param key
     *            A string that identifies the FileFormat.
     * @param fileformat
     *            An instance of the FileFormat to be added.
     * @see #getFileFormat(String)
     * @see #getFileFormatKeys()
     * @see #getFileFormats()
     * @see #removeFileFormat(String)
     */
    public static final void addFileFormat(String key, FileFormat fileformat) {
        if ((fileformat == null) || (key == null))
            return;

        key = key.trim();

        if (!FileList.containsKey(key))
            FileList.put(key, fileformat);
    }

    /**
     * Returns the FileFormat with specified key from the list of supported
     * formats.
     *
     * This method returns a FileFormat instance, as identified by an
     * identifying key, from the list of supported File Formats.
     *
     * If the specified key is in the list of supported formats, the instance of
     * the associated FileFormat object is returned. If the specified key is not
     * in the list of supported formats, <code>null</code> is returned.
     *
     * @param key
     *            A string that identifies the FileFormat.
     * @return The FileFormat that matches the given key, or <code>null</code>
     *         if the key is not found in the list of supported File Formats.
     * @see #addFileFormat(String,FileFormat)
     * @see #getFileFormatKeys()
     * @see #getFileFormats()
     * @see #removeFileFormat(String)
     */
    public static final FileFormat getFileFormat(String key) {
        return FileList.get(key);
    }

    /**
     * Returns an Enumeration of keys for all supported formats.
     *
     * This method returns an Enumeration containing the unique keys (Strings)
     * for the all File Formats in the list of supported File Formats.
     *
     * @return An Enumeration of keys that are in the list of supported formats.
     * @see #addFileFormat(String,FileFormat)
     * @see #getFileFormat(String)
     * @see #getFileFormats()
     * @see #removeFileFormat(String)
     */
    @SuppressWarnings("rawtypes")
    public static final Enumeration getFileFormatKeys() {
        return ((Hashtable) FileList).keys();
    }

    /**
     * Returns an array of supported FileFormat instances.
     *
     * This method returns an array of FileFormat instances that appear in the
     * list of supported File Formats.
     *
     * If the list of supported formats is empty, <code>null</code> is returned.
     *
     * @return An array of all FileFormat instances in the list of supported
     *         File Formats, or <code>null</code> if the list is empty.
     * @see #addFileFormat(String,FileFormat)
     * @see #getFileFormat(String)
     * @see #getFileFormatKeys()
     * @see #removeFileFormat(String)
     */
    @SuppressWarnings("rawtypes")
    public static final FileFormat[] getFileFormats() {
        int n = FileList.size();
        if (n <= 0)
            return null;

        int i = 0;
        FileFormat[] fileformats = new FileFormat[n];
        Enumeration<?> local_enum = ((Hashtable) FileList).elements();
        while (local_enum.hasMoreElements())
            fileformats[i++] = (FileFormat) local_enum.nextElement();

        return fileformats;
    }

    /**
     * Removes a FileFormat from the list of supported formats.
     *
     * This method removes a FileFormat, as identified by the specified key,
     * from the list of supported File Formats.
     *
     * If the specified key is in the list of supported formats, the instance of
     * the FileFormat object that is being removed from the list is returned. If
     * the key is not in the list of supported formats, <code>null</code> is
     * returned.
     *
     * @param key
     *            A string that identifies the FileFormat to be removed.
     * @return The FileFormat that is removed, or <code>null</code> if the key
     *         is not found in the list of supported File Formats.
     * @see #addFileFormat(String,FileFormat)
     * @see #getFileFormat(String)
     * @see #getFileFormatKeys()
     * @see #getFileFormats()
     */
    public static final FileFormat removeFileFormat(String key) {
        return FileList.remove(key);
    }

    /**
     * Adds file extension(s) to the list of file extensions for supported file
     * formats.
     *
     * Multiple extensions can be included in the single parameter if they are
     * separated by commas.
     *
     * The list of file extensions updated by this call is not linked with
     * supported formats that implement FileFormat objects. The file extension
     * list is maintained for the benefit of applications that may choose to
     * recognize only those files with extensions that appear in the list of
     * file extensions for supported file formats.
     *
     * By default, the file extensions list includes: "hdf, h4, hdf5, h5"
     *
     * @param extension
     *            The file extension(s) to add.
     * @see #addFileFormat(String,FileFormat)
     * @see #getFileExtensions()
     */
    public static final void addFileExtension(String extension) {
        if ((extensions == null) || (extensions.length() <= 0))
            extensions = extension;

        StringTokenizer currentExt = new StringTokenizer(extensions, ",");
        Vector<String> tokens = new Vector<>(currentExt.countTokens() + 5);

        while (currentExt.hasMoreTokens())
            tokens.add(currentExt.nextToken().trim().toLowerCase());

        currentExt = new StringTokenizer(extension, ",");
        String ext = null;
        while (currentExt.hasMoreTokens()) {
            ext = currentExt.nextToken().trim().toLowerCase();
            if (tokens.contains(ext))
                continue;

            extensions = extensions + ", " + ext;
        }

        tokens.setSize(0);
    }

    /**
     * Returns a list of file extensions for all supported file formats.
     *
     * The extensions in the returned String are separates by commas:
     * "hdf, h4, hdf5, h5"
     *
     * It is the responsibility of the application to update the file extension
     * list using {@link #addFileExtension(String)} when new FileFormat
     * implementations are added.
     *
     * @return A list of file extensions for all supported file formats.
     * @see #addFileExtension(String)
     */
    public static final String getFileExtensions() {
        return extensions;
    }

    /**
     * Creates a FileFormat instance for the specified file.
     *
     * This method checks the list of supported file formats to find one that
     * matches the format of the specified file. If a match is found, the method
     * returns an instance of the associated FileFormat object. If no match is
     * found, <code>null</code> is returned.
     *
     * For example, if "test_hdf5.h5" is an HDF5 file,
     * FileFormat.getInstance("test_hdf5.h5") will return an instance of H5File.
     *
     * The file is not opened as part of this call. Read/write file access is
     * associated with the FileFormat instance if the matching file format
     * supports read/write access. Some file formats only support read access.
     *
     * @param filename
     *            A valid file name, with a relative or absolute path.
     * @return An instance of the matched FileFormat; <code>null</code> if no
     *         match.
     * @throws IllegalArgumentException
     *             If the <code>filename</code> argument is <code>null</code> or
     *             does not specify an existing file.
     * @throws Exception
     *             If there are problems creating the new instance.
     * @see #createFile(String, int)
     * @see #createInstance(String, int)
     * @see #getFileFormats()
     */
    @SuppressWarnings("rawtypes")
    public static final FileFormat getInstance(String filename) throws Exception {
        if ((filename == null) || (filename.length() <= 0))
            throw new IllegalArgumentException("Invalid file name: " + filename);

        if (!(new File(filename)).exists())
            throw new IllegalArgumentException("File " + filename + " does not exist.");

        FileFormat fileFormat = null;
        FileFormat knownFormat = null;
        Enumeration<?> elms = ((Hashtable) FileList).elements();

        while (elms.hasMoreElements()) {
            knownFormat = (FileFormat) elms.nextElement();
            if (knownFormat.isThisType(filename)) {
                try {
                    fileFormat = knownFormat.createInstance(filename, WRITE);
                }
                catch (Exception ex) {
                    log.debug("File {} createInstance failure: ", filename, ex);
                }
                break;
            }
        }

        return fileFormat;
    }

    /***************************************************************************
     * Implementation Class methods. These methods are related to the
     * implementing FileFormat class, but not to a particular instance of that
     * class. Since we can't override class methods (they can only be shadowed
     * in Java), these are instance methods.
     *
     * The non-abstract methods just throw an exception indicating that the
     * implementing class doesn't support the functionality.
     **************************************************************************/

    /**
     * Returns the version of the library for the implementing FileFormat class.
     *
     * The implementing FileFormat classes have freedom in how they obtain or
     * generate the version number that is returned by this method. The H5File
     * and H4File implementations query the underlying HDF libraries and return
     * the reported version numbers. Other implementing classes may generate the
     * version string directly within the called method.
     *
     * @return The library version.
     */
    public abstract String getLibversion();

    /**
     * Checks if the class implements the specified FileFormat.
     *
     * The Java "instanceof" operation is unable to check if an object is an
     * instance of a FileFormat that is loaded at runtime. This method provides
     * the "instanceof" functionality, and works for implementing classes that
     * are loaded at runtime.
     *
     * This method lets applications that only access the abstract object layer
     * determine the format of a given instance of the abstract class.
     *
     * For example, HDFView uses the following code to determine if a file is an
     * HDF5 file:
     *
     * <pre>
     * FileFormat h5F = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
     * HObject hObject = viewer.getTreeView().getCurrentObject();
     * FileFormat thisF = hObject.getFileFormat();
     * boolean isH5 = h5F.isThisType(thisF);
     * </pre>
     *
     * @param fileFormat
     *            The FileFormat to be checked.
     * @return True if this instance implements the specified FileFormat;
     *         otherwise returns false.
     * @see #isThisType(String)
     */
    public abstract boolean isThisType(FileFormat fileFormat);

    /**
     * Checks if the implementing FileFormat class matches the format of the
     * specified file.
     *
     * For example, if "test.h5" is an HDF5 file, the first call to isThisType()
     * in the code fragment shown will return <code>false</code>, and the second
     * call will return <code>true</code>.
     *
     * <pre>
     * FileFormat ncF = FileFormat.getFileFormat(FileFormat.FILE_TYPE_NC3);
     * FileFormat h4F = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);
     * FileFormat h5F = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
     * boolean isH4 = h4F.isThisType(&quot;test.h5&quot;); // false
     *                                                                                                                                                                                   boolean isH5 = h5F.isThisType(&quot;test.h5&quot;); // true
     * </pre>
     *
     * @param filename
     *            The name of the file to be checked.
     * @return True if the format of the file matches the format of this
     *         instance; otherwise returns false.
     * @see #isThisType(FileFormat)
     */
    public abstract boolean isThisType(String filename);

    /**
     * Creates a file with the specified name and returns a new FileFormat
     * implementation instance associated with the file.
     *
     * This method creates a file whose format is the same as that of the
     * implementing class. An instance of the FileFormat implementing class is
     * created and associated with the file. That instance is returned by the
     * method.
     *
     * The filename in this method call is equivalent to the pathname in the
     * java.io.File class. The filename is converted into an abstract pathname
     * by the File class.
     *
     * A flag controls the behavior if the named file already exists. The flag
     * values and corresponding behaviors are:
     * <ul>
     * <li>FILE_CREATE_DELETE: Create a new file or truncate an existing one.
     * <li>FILE_CREATE_OPEN: Create a new file or open an existing one.
     * </ul>
     *
     * If the flag is FILE_CREATE_DELETE, the method will create a new file or
     * truncate an existing file. If the flag is FILE_CREATE_OPEN and the file
     * does not exist, the method will create a new file.
     *
     * This method does not open the file for access, nor does it confirm that
     * the file can later be opened read/write. The file open is carried out by
     * the <i>open()</i> call.
     *
     * @param filename
     *            The filename; a pathname string.
     * @param createFlag
     *            The creation flag, which determines behavior when the file
     *            already exists. Acceptable values are
     *            <code>FILE_CREATE_DELETE</code> and
     *            <code>FILE_CREATE_OPEN</code>.
     * @throws NullPointerException
     *             If the <code>filename</code> argument is <code>null</code>.
     * @throws UnsupportedOperationException
     *             If the implementing class does not support the file creation operation.
     * @throws Exception
     *             If the file cannot be created or if the creation flag has an
     *             unexpected value. The exceptions thrown vary depending on the
     *             implementing class.
     * @see #createInstance(String, int)
     * @see #getInstance(String)
     * @see #open()
     *
     * @return the FileFormat instance.
     */
    public FileFormat createFile(String filename, int createFlag) throws Exception {
        // If the implementing subclass doesn't have this method then that
        // format doesn't support File Creation and we throw an exception.
        throw new UnsupportedOperationException("FileFormat FileFormat.createFile(...) is not implemented.");
    }

    /**
     * Creates a FileFormat implementation instance with specified filename and
     * access.
     *
     * This method creates an instance of the FileFormat implementing class and
     * sets the filename and file access parameters.
     *
     * The filename in this method call is equivalent to the pathname in the
     * java.io.File class. The filename is converted into an abstract pathname
     * by the File class.
     *
     * The access parameter values and corresponding behaviors at file open:
     * <ul>
     * <li>READ: Read-only access. Fail if file doesn't exist.
     * <li>SWMR: Read-only access. Fail if file doesn't exist.
     * <li>WRITE: Read/Write access. Behavior if file doesn't exist or can't be
     * opened for read/write access depends on the implementing class.
     * <li>CREATE: Read/Write access. Create a new file or truncate an existing
     * one. Behavior if file can't be created, or if it exists but can't be
     * opened read/write depends on the implementing class.
     * </ul>
     *
     * Some FileFormat implementing classes may only support READ access and
     * will use READ regardless of the value specified in the call. Refer to the
     * implementing class documentation for details.
     *
     * This method does not open the file for access, nor does it confirm that
     * the file can later be opened read/write or created. The file open is
     * carried out by the <i>open()</i> call.
     *
     * Example (without exception handling):
     *
     * <pre>
     * // Request the implementing class of FileFormat: H5File
     * FileFormat h5file = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
     *
     * // Create an instance of H5File object with read/write access
     * H5File test1 = (H5File) h5file.createInstance(&quot;test_hdf5.h5&quot;,
     *                                               FileFormat.WRITE);
     *
     * // Open the file and load the file structure; file id is returned.
     * int fid = test1.open();
     * </pre>
     *
     * @param filename
     *            The filename; a pathname string.
     * @param access
     *            The file access flag, which determines behavior when file is
     *            opened. Acceptable values are <code> READ, WRITE, </code> and
     *            <code>CREATE</code>.
     * @throws NullPointerException
     *             If the <code>filename</code> argument is <code>null</code>.
     * @throws Exception
     *             If the instance cannot be created or if the access flag has
     *             an unexpected value. The exceptions thrown vary depending on
     *             the implementing class.
     * @see #createFile(String, int)
     * @see #getInstance(String)
     * @see #open()
     *
     * @return the FileFormat instance.
     */
    public abstract FileFormat createInstance(String filename, int access) throws Exception;

    // REVIEW DOCS for createInstance()
    // What if READ ONLY in implementation? What if file already open?
    // Can we doc exceptions better or in implementation methods?

    /***************************************************************************
     * Final instance methods
     *
     * Related to a given instance of the class, but at the FileFormat level,
     * not at the implementing class level.
     **************************************************************************/

    /**
     * Returns the absolute path for the file.
     *
     * For example, "/samples/hdf5_test.h5". If there is no file associated with
     * this FileFormat instance, <code>null</code> is returned.
     *
     * @return The full path (file path + file name) of the associated file, or
     *         <code>null</code> if there is no associated file.
     */
    public final String getFilePath() {
        return fullFileName;
    }

    /**
     * Returns file identifier of open file associated with this instance.
     *
     * @return The file identifer, or -1 if there is no file open.
     */
    public final long getFID() {
        return fid;
    }

    /**
     * Returns true if the file access is read-only.
     *
     * This method returns true if the file access is read-only. If the file
     * access is read-write, or if there is no file associated with the
     * FileFormat instance, false will be returned.
     *
     * Note that this method may return true even if the file is not open for
     * access when the method is called. The file access is set by the
     * <i>createFile()</i>, <i>createInstance()</i>, or <i>getInstance()</i>
     * call, and the file is opened for access by the <i>open()</i> call.
     *
     * @return True if the file access is read-only, otherwise returns false.
     * @see #createFile(String, int)
     * @see #createInstance(String, int)
     * @see #getInstance(String)
     * @see #open()
     */
    public final boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * Sets the maximum number of objects to be loaded into memory.
     *
     * Current Java applications, such as HDFView, cannot handle files with
     * large numbers of objects due to JVM memory limitations. The maximum
     * number limits the number of objects that will be loaded for a given
     * FileFormat instance.
     *
     * The implementing FileFormat class has freedom in how it interprets the
     * maximum number. H5File, for example, will load the maximum number of
     * objects for each group in the file.
     *
     * @param n
     *            The maximum number of objects to be loaded into memory.
     * @see #getMaxMembers()
     * @see #setStartMembers(int)
     */
    public final void setMaxMembers(int n) {
        max_members = n;
    }

    /**
     * Returns the maximum number of objects that can be loaded into memory.
     *
     * @return The maximum number of objects that can be loaded into memory.
     * @see #setMaxMembers(int)
     */
    public final int getMaxMembers() {
        if (max_members<0)
            return Integer.MAX_VALUE; // load the whole file

        return max_members;
    }

    /**
     * Sets the starting index of objects to be loaded into memory.
     *
     * The implementing FileFormat class has freedom in how it indexes objects
     * in the file.
     *
     * @param idx
     *            The starting index of the object to be loaded into memory
     * @see #getStartMembers()
     * @see #setMaxMembers(int)
     */
    public final void setStartMembers(int idx) {
        start_members = idx;
    }

    /**
     * Returns the index of the starting object to be loaded into memory.
     *
     * @return The index of the starting object to be loaded into memory.
     * @see #setStartMembers(int)
     */
    public final int getStartMembers() {
        return start_members;
    }

    /**
     * Returns the number of objects in memory.
     *
     * This method returns the total number of objects loaded into memory for
     * this FileFormat instance. The method counts the objects that are loaded,
     * which can take some time for a large number of objects.
     *
     * It is worth noting that the total number of objects in memory may be
     * different than the total number of objects in the file.
     *
     * Since implementing classes have freedom in how they interpret and use the
     * maximum number of members value, there may be differing numbers of
     * objects in memory in different implementation instances, even with the
     * same "use case".
     *
     * For example, say the use case is a file that contains 20,000 objects, the
     * maximum number of members for an instance is 10,000, and the start member
     * index is 1. There are 2 groups in the file. The root group contains
     * 10,500 objects and the group "/g1" contains 9,500 objects.
     *
     * In an implementation that limits the total number of objects loaded to
     * the maximum number of members, this method will return 10,000.
     *
     * In contrast, the H5File implementation loads up to the maximum number of
     * members objects for each group in the file. So, with our use case 10,000
     * objects will be loaded in the root group and 9,500 objects will be loaded
     * into group "/g1". This method will return the value 19,500, which exceeds
     * the maximum number of members value.
     *
     * @return The number of objects in memory.
     * @see #getMaxMembers()
     * @see #setMaxMembers(int)
     * @see #getStartMembers()
     * @see #setStartMembers(int)
     */
    public final int getNumberOfMembers() {
        HObject rootObject = getRootObject();

        // Account for root object
        if (rootObject != null)
            return ((Group) rootObject).depthFirstMemberList().size() + 1;

        return 0;
    }

    /***************************************************************************
     * Abstract Instance methods
     *
     * These methods are related to the Implementing FileFormat class and to
     * particular instances of objects with those classes.
     **************************************************************************/

    /**
     * Opens file and returns a file identifier.
     *
     * This method uses the <code>filename</code> and <code>access</code>
     * parameters specified in the <i>createFile()</i>, <i>createInstance()</i>,
     * or <i>getInstance()</i> call to open the file. It returns the file
     * identifier if successful, or a negative value in case of failure.
     *
     * The method also loads the file structure and basic information (name,
     * type) for data objects in the file into the FileFormat instance. It does
     * not load the contents of any data object.
     *
     * The structure of the file is stored in a tree starting from the root
     * object.
     *
     * @return File identifier if successful; otherwise -1.
     * @throws Exception
     *             If the file cannot be opened. The exceptions thrown vary
     *             depending on the implementing class.
     * @see #createFile(String, int)
     * @see #createInstance(String, int)
     * @see #getInstance(String)
     * @see #getRootObject()
     */
    public abstract long open() throws Exception;

    /**
     * Closes file associated with this instance.
     *
     * This method closes the file associated with this FileFormat instance, as
     * well as all objects associated with the file.
     *
     * @throws Exception
     *             If the file or associated objects cannot be closed. The
     *             exceptions thrown vary depending on the implementing class.
     * @see #open()
     */
    public abstract void close() throws Exception;

    // REVIEW DOCS for close()
    // What if we try to close a file whose fid is -1? Does this set fid to -1?
    // What if it's not open? What if no file? are structures & root object
    // still loaded?
    // Can we doc exceptions better or in implementation methods?

    /**
     * Returns the root object for the file associated with this instance.
     *
     * The root object is an HObject that represents the root group of a
     * file. If the file has not yet been opened, or if there is no file
     * associated with this instance, <code>null</code> will be returned.
     *
     * Starting from the root, applications can descend through the tree
     * structure and navigate among the file's objects. In the tree structure,
     * internal items represent non-empty groups. Leaf items represent datasets,
     * named datatypes, or empty groups.
     *
     * @return The root object of the file, or <code>null</code> if there is no
     *         associated file or if the associated file has not yet been opened.
     * @see #open()
     */
    public abstract HObject getRootObject();

    /**
     * Gets the HObject with the specified path from the file.
     *
     * This method returns the specified object from the file associated with
     * this FileFormat instance.
     *
     * If the specified object is a group, groups and datasets that are members
     * of the group will be accessible via the returned HObject instance. The
     * exact contents of the returned HObject instance depends on whether or not
     * {@link #open()} was called previously for this file.
     * <ul>
     * <li>If the file was opened prior to this method call, the complete tree
     * of objects under the group will be accessible via the returned HObject
     * instance.
     * <li>If the file was not opened prior to this method call, only the
     * members immediately under the group will be accessible via the returned
     * HOBject instance.
     * </ul>
     *
     * The decision to have different behaviors was made to give users some
     * control over the "cost" of the method. In many cases, a user wants only
     * one level of a tree, and the performance penalty for loading the entire
     * hierarchy of objects in a large and complex file can be significant. In
     * the case where <i>open()</i> has already been called, the HObject
     * instances have already been created in memory and can be returned
     * quickly. If <i>open()</i> has not been called, this method creates the
     * HObject instances before returning the requested HObject.
     *
     * For example, say we have the following structure in our file:
     *
     * <pre>
     *        /g0                      Group
     *        /g0/dataset_comp         Dataset {50, 10}
     *        /g0/dataset_int          Dataset {50, 10}
     *        /g0/g00                  Group
     *        /g0/g00/dataset_float    Dataset {50, 10}
     *        /g0/g01                  Group
     *        /g0/g01/dataset_string   Dataset {50, 10}
     * </pre>
     *
     * <ul>
     * <li>If <i>open()</i> is called before <i>get()</i>, the full structure of
     * file is loaded into memory. The call <code>get("/g0")</code> returns the
     * instance for /g0 with the information necessary to access
     * /g0/dataset_comp, /g0/dataset_int, /g0/g00, /g0/g00/dataset_float,
     * /g0/g01, and /g0/g01/dataset_string.
     * <li>If <i>open()</i> is not called before <i>get()</i>, only the objects
     * immediately under the specified group are accessible via the returned
     * HObject instance. In this example, the call <code>get("/go")</code>
     * returns the instance for /g0 with the information necessary to access
     * /g0/dataset_comp, /g0/dataset_int, /g0/g00, and /g0/g01.
     * </ul>
     *
     * @param path
     *            Full path of the data object to be returned.
     * @return The object if it exists in the file; otherwise <code>null</code>.
     * @throws Exception
     *             If there are unexpected problems in trying to retrieve the
     *             object. The exceptions thrown vary depending on the
     *             implementing class.
     */
    public abstract HObject get(String path) throws Exception;

    // REVIEW DOCS for get(); What if no file associated w/ instance?
    // Look at exceptions. Confirm example. Make sure perf tradeoffs
    // documented properly.

    /**
     * Creates a named datatype in a file.
     *
     * The following code creates a named datatype in a file.
     *
     * <pre>
     * H5File file = (H5File) h5file.createInstance(&quot;test_hdf5.h5&quot;, FileFormat.WRITE);
     * H5Datatype dtype = file.createNamedDatatype(
     *                             nativetype,
     *                             &quot;Native Integer&quot;);
     * </pre>
     * @param tnative
     *            the native datatype of the new datatype
     * @param name
     *            name of the datatype to create, e.g. "Native Integer".
     * @return The new datatype if successful; otherwise returns null.
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public Datatype createNamedDatatype(Datatype tnative, String name) throws Exception {
        // If the implementing subclass doesn't have this method then that
        // format doesn't support Named Datatypes and we throw an exception.
        throw new UnsupportedOperationException("Datatype FileFormat.createNamedDatatype(...) is not implemented.");
    }

    // REVIEW DOCS for createDatatype(). Check and document exceptions.

    /***************************************************************************
     * Methods related to Datatypes and HObjects in the implementing FileFormat.
     *
     * Strictly speaking, these methods aren't related to FileFormat and the
     * actions could be carried out through the HObject and Datatype classes.
     * But, in some cases they allow a null input and expect the generated
     * object to be of a type that has particular FileFormat. Therefore, we put
     * them in the implementing FileFormat class so that we create the proper
     * type of HObject... H5Group or H4Group for example.
     *
     * Here again, if there could be Implementation Class methods we'd use
     * those. But, since we can't override class methods (they can only be
     * shadowed in Java), these are instance methods.
     *
     * The non-abstract methods just throw an exception indicating that the
     * implementing class doesn't support the functionality.
     **************************************************************************/

    /**
     * Creates a new datatype in memory.
     *
     * The following code creates an instance of H5Datatype in memory.
     *
     * <pre>
     * H5File file = (H5File) h5file.createInstance(&quot;test_hdf5.h5&quot;, FileFormat.WRITE);
     * H5Datatype dtype = file.createDatatype(
     *                             Datatype.CLASS_INTEGER,
     *                             4,
     *                             Datatype.NATIVE,
     *                             Datatype.NATIVE);
     * </pre>
     *
     * @param tclass
     *            class of datatype, e.g. Datatype.CLASS_INTEGER
     * @param tsize
     *            size of the datatype in bytes, e.g. 4 for 32-bit integer.
     * @param torder
     *            order of the byte endian, e.g. Datatype.ORDER_LE.
     * @param tsign
     *            signed or unsigned of an integer, e.g. Datatype.SIGN_NONE.
     * @return The new datatype object if successful; otherwise returns null.
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing
     *             class.
     */
    public abstract Datatype createDatatype(int tclass, int tsize, int torder, int tsign) throws Exception;

    /**
     * Creates a new datatype in memory.
     *
     * The following code creates an instance of H5Datatype in memory.
     *
     * <pre>
     * H5File file = (H5File) h5file.createInstance(&quot;test_hdf5.h5&quot;, FileFormat.WRITE);
     * H5Datatype dtype = file.createDatatype(
     *                             Datatype.CLASS_INTEGER,
     *                             4,
     *                             Datatype.NATIVE,
     *                             Datatype.NATIVE,
     *                             basetype);
     * </pre>
     *
     * @param tclass
     *            class of datatype, e.g. Datatype.CLASS_INTEGER
     * @param tsize
     *            size of the datatype in bytes, e.g. 4 for 32-bit integer.
     * @param torder
     *            order of the byte endian, e.g. Datatype.ORDER_LE.
     * @param tsign
     *            signed or unsigned of an integer, e.g. Datatype.SIGN_NONE.
     * @param tbase
     *            the base datatype of the new datatype
     * @return The new datatype object if successful; otherwise returns null.
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing
     *             class.
     */
    public Datatype createDatatype(int tclass, int tsize, int torder, int tsign, Datatype tbase) throws Exception {
        // Derived classes must override this function to use base type option
        return createDatatype(tclass, tsize, torder, tsign);
    }

    // REVIEW DOCS for createDatatype(). Check and document exceptions.

    /**
     * Creates a new dataset in a file with/without chunking/compression.
     *
     * The following example creates a 2D integer dataset of size 100X50 at the root group in an HDF5
     * file.
     *
     * <pre>
     * String name = &quot;2D integer&quot;;
     * Group pgroup = (Group) getRootObject();
     * Datatype dtype = new H5Datatype(Datatype.CLASS_INTEGER, // class
     *         4, // size in bytes
     *         Datatype.ORDER_LE, // byte order
     *         Datatype.SIGN_NONE); // unsigned
     * long[] dims = { 100, 50 };
     * long[] maxdims = dims;
     * long[] chunks = null; // no
     * // chunking
     * int gzip = 0; // no compression
     * Object data = null; // no initial data values
     * Dataset d = (H5File) file.createScalarDS(name, pgroup, dtype, dims, maxdims, chunks, gzip, data);
     * </pre>
     *
     * @param name
     *            name of the new dataset, e.g. "2D integer"
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param type
     *            datatype of the new dataset.
     * @param dims
     *            dimension sizes of the new dataset, e.g. long[] dims = {100, 50}.
     * @param maxdims
     *            maximum dimension sizes of the new dataset, null if maxdims is the same as dims.
     * @param chunks
     *            chunk sizes of the new dataset, null if no chunking.
     * @param gzip
     *            GZIP compression level (1 to 9), 0 or negative values if no compression.
     * @param fillValue
     *            default value.
     * @param data
     *            data written to the new dataset, null if no data is written to the new dataset.
     *
     * @return The new dataset if successful; otherwise returns null
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public abstract Dataset createScalarDS(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks,
            int gzip, Object fillValue, Object data) throws Exception;

    /**
     * Creates a new dataset in a file with/without chunking/compression.
     *
     * @param name
     *            name of the new dataset, e.g. "2D integer"
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param type
     *            datatype of the new dataset.
     * @param dims
     *            dimension sizes of the new dataset, e.g. long[] dims = {100, 50}.
     * @param maxdims
     *            maximum dimension sizes of the new dataset, null if maxdims is the same as dims.
     * @param chunks
     *            chunk sizes of the new dataset, null if no chunking.
     * @param gzip
     *            GZIP compression level (1 to 9), 0 or negative values if no compression.
     * @param data
     *            data written to the new dataset, null if no data is written to the new dataset.
     *
     * @return The new dataset if successful; otherwise returns null
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public Dataset createScalarDS(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks,
            int gzip, Object data) throws Exception {
        return createScalarDS(name, pgroup, type, dims, maxdims, chunks, gzip, null, data);
    }

    // REVIEW DOCS for createScalarDS(). Check and document exceptions.

    /**
     * Creates a new compound dataset in a file with/without chunking and
     * compression.
     *
     * The following example creates a compressed 2D compound dataset with size
     * of 100X50 in a root group. The compound dataset has two members, x and y.
     * Member x is an interger, member y is an 1-D float array of size 10.
     *
     * <pre>
     * String name = "2D compound";
     * Group pgroup = (Group) getRootObject();
     * long[] dims = {100, 50};
     * long[] chunks = {1, 50};
     * int gzip = 9;
     * String[] memberNames = {"x", "y"};
     *
     * Datatype[] memberDatatypes = {
     *     new H5Datatype(Datatype.CLASS_INTEGER, Datatype.NATIVE,
     *                    Datatype.NATIVE, Datatype.NATIVE)
     *     new H5Datatype(Datatype.CLASS_FLOAT, Datatype.NATIVE,
     *                    Datatype.NATIVE, Datatype.NATIVE));
     *
     * int[] memberSizes = {1, 10};
     * Object data = null; // no initial data values
     * Dataset d = (H5File)file.createCompoundDS(name, pgroup, dims, null,
     *           chunks, gzip, memberNames, memberDatatypes, memberSizes, null);
     * </pre>
     *
     * @param name
     *            name of the new dataset
     * @param pgroup
     *            parent group where the new dataset is created.
     * @param dims
     *            dimension sizes of the new dataset.
     * @param maxdims
     *            maximum dimension sizes of the new dataset, null if maxdims is
     *            the same as dims.
     * @param chunks
     *            chunk sizes of the new dataset, null if no chunking.
     * @param gzip
     *            GZIP compression level (1 to 9), 0 or negative values if no
     *            compression.
     * @param memberNames
     *            names of the members.
     * @param memberDatatypes
     *            datatypes of the members.
     * @param memberSizes
     *            array sizes of the members.
     * @param data
     *            data written to the new dataset, null if no data is written to
     *            the new dataset.
     *
     * @return new dataset object if successful; otherwise returns null
     * @throws UnsupportedOperationException
     *             If the implementing class does not support compound datasets.
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing
     *             class.
     */
    public Dataset createCompoundDS(String name, Group pgroup,
            long[] dims, long[] maxdims, long[] chunks,
            int gzip, String[] memberNames, Datatype[] memberDatatypes, int[] memberSizes, Object data) throws Exception {
        // If the implementing subclass doesn't have this method then that
        // format doesn't support Compound DataSets and we throw an
        // exception.
        throw new UnsupportedOperationException("Dataset FileFormat.createCompoundDS(...) is not implemented.");
    }

    /**
     * Creates a new image in a file.
     *
     * The following example creates a 2D image of size 100X50 in a root group.
     *
     * <pre>
     * String name = &quot;2D image&quot;;
     * Group pgroup = (Group) getRootObject();
     * Datatype dtype = new H5Datatype(Datatype.CLASS_INTEGER, 1, Datatype.NATIVE, Datatype.SIGN_NONE);
     * long[] dims = {100, 50};
     * long[] maxdims = dims;
     * long[] chunks = null; // no chunking
     * int gzip = 0; // no compression
     * int ncomp = 3; // RGB true color image
     * int interlace = ScalarDS.INTERLACE_PIXEL;
     * Object data = null; // no initial data values
     * Dataset d = (H5File) file.createScalarDS(name, pgroup, dtype, dims, maxdims, chunks, gzip, ncomp, interlace, data);
     * </pre>
     *
     * @param name
     *            name of the new image, "2D image".
     * @param pgroup
     *            parent group where the new image is created.
     * @param type
     *            datatype of the new image.
     * @param dims
     *            dimension sizes of the new dataset, e.g. long[] dims = {100,
     *            50}.
     * @param maxdims
     *            maximum dimension sizes of the new dataset, null if maxdims is
     *            the same as dims.
     * @param chunks
     *            chunk sizes of the new dataset, null if no chunking.
     * @param gzip
     *            GZIP compression level (1 to 9), 0 or negative values if no
     *            compression.
     * @param ncomp
     *            number of components of the new image, e.g. int ncomp = 3; //
     *            RGB true color image.
     * @param interlace
     *            interlace mode of the image. Valid values are
     *            ScalarDS.INTERLACE_PIXEL, ScalarDS.INTERLACE_PLANEL and
     *            ScalarDS.INTERLACE_LINE.
     * @param data
     *            data value of the image, null if no data.
     *
     * @return The new image object if successful; otherwise returns null
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing
     *             class.
     */
    public abstract Dataset createImage(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks,
            int gzip, int ncomp, int interlace, Object data) throws Exception;

    // REVIEW DOCS for createImage(). Check and document exceptions.

    /**
     * Creates a new group with specified name in existing group.
     *
     * If the parent group is null, the new group will be created in the root
     * group.
     *
     * @param name
     *            The name of the new group.
     * @param parentGroup
     *            The parent group, or null.
     *
     * @return The new group if successful; otherwise returns null.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing
     *             class.
     */
    public abstract Group createGroup(String name, Group parentGroup) throws Exception;

    // REVIEW DOCS for createLink().
    // Verify Implementing classes document these and also
    // 'do the right thing' if fid is -1, currentObj is non-null, if
    // object is null, or the root group then what? document & verify!

    /**
     * Creates a soft, hard or external link to an existing object in the open file.
     *
     * If parentGroup is null, the new link is created in the root group.
     *
     * @param parentGroup
     *            The group where the link is created.
     * @param name
     *            The name of the link.
     * @param currentObj
     *            The existing object the new link will reference.
     * @param type
     *            The type of link to be created. It can be a hard link, a soft
     *            link or an external link.
     *
     * @return The object pointed to by the new link if successful; otherwise
     *         returns null.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public HObject createLink(Group parentGroup, String name, HObject currentObj, int type) throws Exception {
        return createLink(parentGroup, name, currentObj);
    }

    /**
     * Creates a soft or external link to an object in a file that does not exist
     * at the time the link is created.
     *
     * @param parentGroup
     *            The group where the link is created.
     * @param name
     *            The name of the link.
     * @param currentObj
     *            The name of the object the new link will reference. The object
     *            doesn't have to exist.
     * @param type
     *            The type of link to be created.
     *
     * @return The H5Link object pointed to by the new link if successful;
     *         otherwise returns null.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public HObject createLink(Group parentGroup, String name, String currentObj, int type) throws Exception {
        return createLink(parentGroup, name, currentObj);
    }

    /**
     * Copies the source object to a new destination.
     *
     * This method copies the source object to a destination group, and assigns
     * the specified name to the new object.
     *
     * The copy may take place within a single file or across files. If the source
     * object and destination group are in different files, the files must have
     * the same file format (both HDF5 for example).
     *
     * The source object can be a group, a dataset, or a named datatype. This
     * method copies the object along with all of its attributes and other
     * properties. If the source object is a group, this method also copies all
     * objects and sub-groups below the group.
     *
     * The following example shows how to use the copy method to create two
     * copies of an existing HDF5 file structure in a new HDF5 file. One copy
     * will be under /copy1 and the other under /copy2 in the new file.
     *
     * <pre>
     * // Open the existing file with the source object.
     * H5File existingFile = new H5File(&quot;existingFile.h5&quot;, FileFormat.READ);
     * existingFile.open();
     * // Our source object will be the root group.
     * HObject srcObj = existingFile.get(&quot;/&quot;);
     * // Create a new file.
     * H5File newFile = new H5File(&quot;newFile.h5&quot;, FileFormat.CREATE);
     * newFile.open();
     * // Both copies in the new file will have the root group as their
     * // destination group.
     * Group dstGroup = (Group) newFile.get(&quot;/&quot;);
     * // First copy goes to &quot;/copy1&quot; and second goes to &quot;/copy2&quot;.
     * // Notice that we can use either H5File instance to perform the copy.
     * HObject copy1 = existingFile.copy(srcObj, dstGroup, &quot;copy1&quot;);
     * HObject copy2 = newFile.copy(srcObj, dstGroup, &quot;copy2&quot;);
     * // Close both the files.
     * file.close();
     * newFile.close();
     * </pre>
     *
     * @param srcObj
     *            The object to copy.
     * @param dstGroup
     *            The destination group for the new object.
     * @param dstName
     *            The name of the new object. If dstName is null, the name of
     *            srcObj will be used.
     *
     * @return The new object, or null if the copy fails.
     *
     * @throws Exception
     *             are specific to the implementing class.
     */
    public abstract HObject copy(HObject srcObj, Group dstGroup, String dstName) throws Exception;

    /**
     * Deletes an object from a file.
     *
     * @param obj
     *            The object to delete.
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public abstract void delete(HObject obj) throws Exception;

    // REVIEW DOCS for delete(). Check and document exceptions.

    /**
     * Attaches a given attribute to an object.
     *
     * If an HDF(4&amp;5) attribute exists in file, the method updates its value. If
     * the attribute does not exist in file, it creates the attribute in file
     * and attaches it to the object. It will fail to write a new attribute to
     * the object where an attribute with the same name already exists. To
     * update the value of an existing attribute in file, one needs to get the
     * instance of the attribute by getMetadata(), change its values, and use
     * writeAttribute() to write the value.
     *
     * @param obj
     *            The object to which the attribute is attached to.
     * @param attr
     *            The atribute to attach.
     * @param attrExisted
     *            The indicator if the given attribute exists.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public abstract void writeAttribute(HObject obj, Attribute attr, boolean attrExisted) throws Exception;

    // REVIEW DOCS for writeAttribute(). Check and document exceptions.

    /***************************************************************************
     * Deprecated methods.
     **************************************************************************/

    /**
     * @deprecated As of 2.4, replaced by {@link #createFile(String, int)}
     *
     *             The replacement method has an additional parameter that
     *             controls the behavior if the file already exists. Use
     *             <code>FileFormat.FILE_CREATE_DELETE</code> as the second
     *             argument in the replacement method to mimic the behavior
     *             originally provided by this method.
     *
     * @param fileName
     *            The filename; a pathname string.
     *
     * @return the created file object
     *
     * @throws Exception if file cannot be created
     */
    @Deprecated
    public final FileFormat create(String fileName) throws Exception {
        return createFile(fileName, FileFormat.FILE_CREATE_DELETE);
    }

    /**
     * @deprecated As of 2.4, replaced by {@link #createInstance(String, int)}
     *
     *             The replacement method has identical functionality and a more
     *             descriptive name. Since <i>open</i> is used elsewhere to
     *             perform a different function this method has been deprecated.
     *
     * @param pathname
     *            The pathname string.
     * @param access
     *            The file access properties
     *
     * @return the opened file object
     *
     * @throws Exception if the file cannot be opened
     */
    @Deprecated
    public final FileFormat open(String pathname, int access) throws Exception {
        return createInstance(pathname, access);
    }

    /**
     * @deprecated As of 2.4, replaced by
     *             {@link #createCompoundDS(String, Group, long[], long[], long[], int, String[], Datatype[], int[], Object)}
     *
     *             The replacement method has additional parameters:
     *             <code>maxdims, chunks,</code> and <code>gzip</code>. To mimic
     *             the behavior originally provided by this method, call the
     *             replacement method with the following parameter list:
     *             <code> ( name, pgroup, dims, null, null, -1,
     * memberNames, memberDatatypes, memberSizes, data ); </code>
     *
     * @param name
     *            The dataset name.
     * @param pgroup
     *            The dataset parent.
     * @param dims
     *            The dataset dimensions.
     * @param memberNames
     *            The dataset compound member names.
     * @param memberDatatypes
     *            The dataset compound member datatypes.
     * @param memberSizes
     *            The dataset compound member sizes.
     * @param data
     *            The dataset data.
     *
     * @return
     *            The dataset created.
     *
     * @throws Exception if the dataset cannot be created
     */
    @Deprecated
    public final Dataset createCompoundDS(String name, Group pgroup, long[] dims,
            String[] memberNames, Datatype[] memberDatatypes, int[] memberSizes, Object data) throws Exception {
        return createCompoundDS(name, pgroup, dims, null, null, -1, memberNames, memberDatatypes, memberSizes, data);
    }

    /**
     * @deprecated As of 2.4, replaced by {@link #copy(HObject, Group, String)}
     *
     *             To mimic the behavior originally provided by this method,
     *             call the replacement method with <code>null</code> as the 3rd parameter.
     *
     * @param srcObj
     *             The object to be copied
     * @param dstGroup
     *             The group to contain the copied object
     *
     * @return the copied object
     *
     * @throws Exception if object can not be copied
     */
    @Deprecated
    public final HObject copy(HObject srcObj, Group dstGroup) throws Exception {
        return copy(srcObj, dstGroup, null);
    }

    /**
     * @deprecated As of 2.4, replaced by {@link #get(String)}
     *
     *             This static method, which as been deprecated, causes two problems:
     *             <ul>
     *             <li>It can be very expensive if it is called many times or in
     *             a loop because each call to the method creates an instance of a file.
     *             <li>Since the method does not return the instance of the
     *             file, the file cannot be closed directly and may be left open
     *             (memory leak). The only way to close the file is through the
     *             object returned by this method.
     *             </ul>
     *
     * @param fullPath
     *            The file path string.
     *
     * @return the object that has the given full path
     *
     * @throws Exception if the object can not be found
     */
    @Deprecated
    public static final HObject getHObject(String fullPath) throws Exception {
        if ((fullPath == null) || (fullPath.length() <= 0))
            return null;

        String filename = null, path = null;
        int idx = fullPath.indexOf(FILE_OBJ_SEP);

        if (idx > 0) {
            filename = fullPath.substring(0, idx);
            path = fullPath.substring(idx + FILE_OBJ_SEP.length());
            if ((path == null) || (path.length() == 0))
                path = "/";
        }
        else {
            filename = fullPath;
            path = "/";
        }

        return FileFormat.getHObject(filename, path);
    };

    /**
     * @deprecated As of 2.4, replaced by {@link #get(String)}
     *
     *             This static method, which as been deprecated, causes two problems:
     *             <ul>
     *             <li>It can be very expensive if it is called many times or in
     *             a loop because each call to the method creates an instance of
     *             a file.
     *             <li>Since the method does not return the instance of the
     *             file, the file cannot be closed directly and may be left open
     *             (memory leak). The only way to close the file is through the
     *             object returned by this method, for example:
     *             <pre>
     * Dataset dset = H5File.getObject("hdf5_test.h5", "/images/iceburg");
     * ...
     * // close the file through dset
     * dset.getFileFormat().close();
     * </pre>
     *
     *             </li>
     *             </ul>
     *
     * @param filename
     *            The filename string.
     * @param path
     *            The path of the file
     *
     * @return the object that has the given filename and path returns null
     *
     * @throws Exception if the object can not be found
     */
    @Deprecated
    public static final HObject getHObject(String filename, String path) throws Exception {
        if ((filename == null) || (filename.length() <= 0))
            throw new IllegalArgumentException("Invalid file name. " + filename);

        if (!(new File(filename)).exists())
            throw new IllegalArgumentException("File does not exists");

        HObject obj = null;
        FileFormat file = FileFormat.getInstance(filename);

        if (file != null) {
            obj = file.get(path);
            if (obj == null)
                file.close();
        }

        return obj;
    }

    /**
     * Finds an object by its object ID
     *
     * @param file
     *            the file containing the object
     * @param oid
     *            the oid to search for
     *
     * @return the object that has the given OID; otherwise returns null
     */
    public static final HObject findObject(FileFormat file, long[] oid) {
        if ((file == null) || (oid == null)) {
            log.debug("findObject(): file is null or oid is null");
            return null;
        }

        HObject theObj = null;

        HObject theRoot = file.getRootObject();
        if (theRoot == null) {
            log.debug("findObject(): rootObject is null");
            return null;
        }

        Iterator<HObject> member_it = ((Group) theRoot).breadthFirstMemberList().iterator();
        while (member_it.hasNext()) {
            theObj = member_it.next();
            if (theObj.equalsOID(oid))
                break;
        }

        return theObj;
    }

    /**
     * Finds an object by the full path of the object (path+name)
     *
     * @param file
     *            the file containing the object
     * @param path
     *            the full path of the object to search for
     *
     * @return the object that has the given path; otherwise returns null
     */
    public static final HObject findObject(FileFormat file, String path) {
        log.trace("findObject({}): start", path);

        if ((file == null) || (path == null)) {
            log.debug("findObject(): file is null or path is null");
            return null;
        }

        if (!path.endsWith("/"))
            path = path + "/";

        HObject theRoot = file.getRootObject();

        if (theRoot == null) {
            log.debug("findObject(): rootObject is null");
            return null;
        }
        else if (path.equals("/")) {
            log.debug("findObject() path is rootObject");
            return theRoot;
        }

        Iterator<HObject> member_it = ((Group) theRoot).breadthFirstMemberList().iterator();
        HObject theObj = null;
        while (member_it.hasNext()) {
            theObj = member_it.next();
            String fullPath = theObj.getFullName() + "/";

            if (path.equals(fullPath) && theObj.getPath() != null)
                break;
            else
                theObj = null;
        }

        return theObj;
    }

    // ////////////////////////////////////////////////////////////////////////////////////
    // Added to support HDF5 1.8 features //
    // ////////////////////////////////////////////////////////////////////////////////////

    /**
     * Opens file and returns a file identifier.
     *
     * @param indexList
     *            The property list is the list of parameters, like index type
     *            and the index order. The index type can be alphabetical or
     *            creation. The index order can be increasing order or
     *            decreasing order.
     *
     * @return File identifier if successful; otherwise -1.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public long open(int... indexList) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:open.");
    }

    /**
     * Creates a new group with specified name in existing group.
     *
     * If the parent group is null, the new group will be created in the root
     * group.
     *
     * @param name
     *            The name of a new group.
     * @param pgroup
     *            The parent group object.
     * @param gplist
     *            The group creation properties, in which the order of the
     *            properties conforms the HDF5 library API, H5Gcreate(), i.e.
     *            lcpl, gcpl and gapl, where
     *            <ul>
     *            <li>lcpl : Property list for link creation <li>gcpl : Property
     *            list for group creation <li>gapl : Property list for group access
     *            </ul>
     *
     * @return The new group if successful; otherwise returns null.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public Group createGroup(String name, Group pgroup, long... gplist) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:createGroup.");
    }

    /***
     * Creates the group creation property list identifier, gcpl. This
     * identifier is used when creating Groups.
     *
     * @param creationorder
     *            The order in which the objects in a group should be created.
     *            It can be Tracked or Indexed.
     * @param maxcompact
     *            The maximum number of links to store in the group in a compact format.
     * @param mindense
     *            The minimum number of links to store in the indexed
     *            format.Groups which are in indexed format and in which the
     *            number of links falls below this threshold are automatically
     *            converted to compact format.
     *
     * @return The gcpl identifier.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public long createGcpl(int creationorder, int maxcompact, int mindense) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:createGcpl.");
    }

    /**
     * Creates a link to an existing object in the open file.
     *
     * If linkGroup is null, the new link is created in the root group.
     *
     * @param linkGroup
     *            The group where the link is created.
     * @param name
     *            The name of the link.
     * @param currentObj
     *            The existing object the new link will reference.
     *
     * @return The object pointed to by the new link if successful;
     *         otherwise returns null.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public HObject createLink(Group linkGroup, String name, Object currentObj) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:createLink.");
    }

    /**
     * Export dataset.
     *
     * @param file_export_name
     *            The file name to export data into.
     * @param object
     *            The HDF5 dataset object.
     * @param binary_order
     *            The data byte order
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public void exportDataset(String file_export_name, Dataset object, int binary_order) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:exportDataset.");
    }

    /**
     * Renames an attribute.
     *
     * @param obj
     *            The object whose attribute is to be renamed.
     * @param oldAttrName
     *            The current name of the attribute.
     * @param newAttrName
     *            The new name of the attribute.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public void renameAttribute(HObject obj, String oldAttrName, String newAttrName) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:renameAttribute.");
    }

    /**
     * Sets the bounds of new library versions.
     *
     * @param lowStr
     *            The earliest version of the library.
     * @param highStr
     *            The latest version of the library.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public void setNewLibBounds(String lowStr, String highStr) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:setNewLibBounds.");
    }

    /**
     * Sets the bounds of library versions.
     *
     * @param lowStr
     *            The earliest version of the library.
     * @param highStr
     *            The latest version of the library.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public void setLibBounds(String lowStr, String highStr) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:setLibBounds.");
    }

    /**
     * Gets the bounds of library versions
     *
     * @return The earliest and latest library versions in an int array.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public int[] getLibBounds() throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:getLibBounds.");
    }

    /**
     * Initialize the bounds of library versions
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public void initLibBounds() throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:initLibBounds.");
    }

    /**
     * Gets the bounds of library versions as text.
     *
     * @return libversion The earliest and latest version of the library.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public String getLibBoundsDescription() throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:getLibBoundsDescription.");
    }

    /** @return the int value of the index type value.
     *
     * @param strtype
     *            The name of the index type.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public static int getIndexTypeValue(String strtype) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:getIndexTypeValue.");
    }

    /** @return the int value of the index type.
     *
     * @param strtype
     *            The name of the index type.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public int getIndexType(String strtype) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:getIndexType.");
    }

    /** set the int value of the index type.
     *
     * @param indexType
     *            The value of the index type.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public void setIndexType(int indexType) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:setIndexType.");
    }

    /** @return the int value of the index order value.
     *
     * @param strorder
     *            The name of the index order.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public static int getIndexOrderValue(String strorder) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:getIndexOrderValue.");
    }

    /** @return the int value of the index order.
     *
     * @param strorder
     *            The name of the index order.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public int getIndexOrder(String strorder) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:getIndexOrder.");
    }

    /** set the int value of the index order.
     *
     * @param indexOrder
     *            The index order.
     *
     * @throws Exception
     *             The exceptions thrown vary depending on the implementing class.
     */
    public void setIndexOrder(int indexOrder) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation. Subclasses must implement FileFormat:setIndexOrder.");
    }
}
