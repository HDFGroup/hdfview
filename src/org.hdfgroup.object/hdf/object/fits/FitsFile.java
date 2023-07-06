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

package hdf.object.fits;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hdf.object.Attribute;
import hdf.object.Dataset;
import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.HObject;

import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.RandomGroupsHDU;
import nom.tam.fits.TableHDU;

/**
 * This class provides file level APIs. File access APIs include retrieving the
 * file hierarchy, opening and closing file, and writing file content to disk.
 *
 * @version 2.4 9/4/2007
 * @author Peter X. Cao
 */
public class FitsFile extends FileFormat
{
    private static final long serialVersionUID = -1965689032980605791L;

    private static final Logger log = LoggerFactory.getLogger(FitsFile.class);

    /**
     * The root object of the file hierarchy.
     */
    private HObject rootObject;

    /** the fits file */
    private Fits fitsFile;

    private static boolean isFileOpen;

    /**
     * Constructs an empty FitsFile with read-only access.
     */
    public FitsFile() {
        this("");
    }

    /**
     * Constructs an FitsFile object of given file name with read-only access.
     *
     * @param pathname the file name.
     */
    public FitsFile(String pathname) {
        super(pathname);

        isReadOnly = true;
        isFileOpen = false;
        this.fid = -1;
        try {
            fitsFile = new Fits(fullFileName);
        }
        catch (Exception ex) {
            if(!pathname.isEmpty())
                log.debug("Fits({}):", fullFileName, ex);
        }
    }


    /**
     * Checks if the given file format is a Fits file.
     *
     * @param fileformat the fileformat to be checked.
     *
     * @return true if the given file is an Fits file; otherwise returns false.
     */
    @Override
    public boolean isThisType(FileFormat fileformat) {
        return (fileformat instanceof FitsFile);
    }

    /**
     * Checks if a given file is a Fits file.
     *
     * @param filename the file to be checked.
     *
     * @return true if the given file is an Fits file; otherwise returns false.
     */
    @Override
    public boolean isThisType(String filename) {
        boolean is_fits = false;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(filename, "r");
        }
        catch (Exception ex) {
            raf = null;
        }

        if (raf == null)
            return false;

        byte[] header = new byte[80];
        try {
            raf.read(header);
        }
        catch (Exception ex) {
            header = null;
        }

        if (header != null) {
            String front = new String(header, 0, 9);
            if (!front.startsWith("SIMPLE  =")) {
                try {
                    raf.close();
                }
                catch (Exception ex) {
                    log.debug("closing RandomAccessFile({}):", filename, ex);
                }
                return false;
            }

            String back = new String(header, 9, 70);
            back = back.trim();
            if ((back.length() < 1) || (back.charAt(0) != 'T')) {
                try {
                    raf.close();
                }
                catch (Exception ex) {
                    log.debug("closing RandomAccessFile({}):", filename, ex);
                }
                return false;
            }

            is_fits = true;;
        }

        try {
            raf.close();
        }
        catch (Exception ex) {
            log.debug("closing RandomAccessFile({}):", filename, ex);
        }

        return is_fits;
    }


    /**
     * Creates a FitsFile instance with specified file name and READ access.
     *
     * @param filename the full path name of the file.
     * @param access the access properties of the file.
     * Regardless of specified access, the FitsFile implementation uses* READ.
     *
     * @return the Fits file.
     *
     * @throws Exception
     *             The exception thrown from the File class.
     */
    @Override
    public FileFormat createInstance(String filename, int access) throws Exception {
        return new FitsFile(filename);
    }


    // Implementing FileFormat
    @Override
    public long open() throws Exception {
        if (!isFileOpen) {
            isFileOpen = true;
            rootObject = loadTree();
        }

        return 0;
    }

    private HObject loadTree() {
        long[] oid = {0};
        // root object does not have a parent path or a parent node
        FitsGroup rootGroup = new FitsGroup(this, "/", null, null, oid);

        if (fitsFile == null)
            return rootGroup;

        BasicHDU[] hdus = null;

        try {
            hdus = fitsFile.read();
        }
        catch (Exception ex) {
            log.debug("fitsFile.read():", ex);
        }

        if (hdus == null)
            return rootGroup;

        int n = hdus.length;
        int nImageHDU = 0;
        int nTableHDU =  0;
        String hduName = null;
        BasicHDU hdu = null;
        for (int i=0; i<n; i++) {
            hdu = hdus[i];
            hduName = null;
            // only deal with ImageHDU and TableHDU
            if (hdu instanceof ImageHDU) {
                hduName = "ImageHDU #"+nImageHDU++;
            }
            else if (hdu instanceof RandomGroupsHDU) {
                hduName = "RandomGroupsHDU #"+nImageHDU++;
            }
            else if (hdu instanceof TableHDU) {
                if (hdu instanceof AsciiTableHDU)
                    hduName = "AsciiTableHDU #"+nTableHDU++;
                else if (hdu instanceof BinaryTableHDU)
                    hduName = "BinaryTableHDU #"+nTableHDU++;
                else
                    hduName = "TableHDU #"+nTableHDU++;
            }

            if (hduName != null) {
                oid[0] = hdu.hashCode();
                FitsDataset d =  new FitsDataset(this, hdu, hduName, oid);
                rootGroup.addToMemberList(d);
            }
        }

        return rootGroup;
    }

    // Implementing FileFormat
    @Override
    public void close() throws IOException {
        if (fitsFile == null)
            return;

        DataInput di = fitsFile.getStream();
        if (di instanceof InputStream)
            ((InputStream)di).close();
    }

    // Implementing FileFormat
    @Override
    public HObject getRootObject() {
        return rootObject;
    }

    /**
    * @return the Fits file.
    */
    public Fits getFitsFile() {
        return fitsFile;
    }

    // implementign FileFormat
    @Override
    public Group createGroup(String name, Group pgroup) throws Exception {
        throw new UnsupportedOperationException("Unsupported createGroup operation for Fits.");
    }

    // implementign FileFormat
    @Override
    public Datatype createDatatype(int tclass, int tsize, int torder, int tsign) throws Exception {
        throw new UnsupportedOperationException("Unsupported createDatatype operation for Fits.");
    }

    // implementign FileFormat
    @Override
    public Datatype createNamedDatatype(Datatype tnative, String name) throws Exception {
        throw new UnsupportedOperationException("Fits does not support named datatype.");
    }

    // implementign FileFormat
    @Override
    public Dataset createScalarDS(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks,
            int gzip, Object fillValue, Object data) throws Exception {
        throw new UnsupportedOperationException("Unsupported createScalarDS operation.");
    }

    // implementign FileFormat
    @Override
    public Dataset createImage(String name, Group pgroup, Datatype type,
            long[] dims, long[] maxdims, long[] chunks,
            int gzip, int ncomp, int intelace, Object data) throws Exception {
        throw new UnsupportedOperationException("Unsupported createImage operation.");
    }

    // implementing FileFormat
    @Override
    public void delete(HObject obj) throws Exception {
        throw new UnsupportedOperationException("Unsupported delete operation.");
    }

    // implementing FileFormat
    @Override
    public HObject copy(HObject srcObj, Group dstGroup, String dstName) throws Exception {
        throw new UnsupportedOperationException("Unsupported copy operation.");
    }

    /**
     * copy a dataset into another group.
     *
     * @param srcDataset the dataset to be copied.
     * @param pgroup the group where the dataset is copied to.
     *
     * @return the treeNode containing the new copy of the dataset.
     */
    private void copyDataset(Dataset srcDataset, FitsGroup pgroup) throws Exception {
        throw new UnsupportedOperationException("Unsupported copyDataset operation.");
    }

    private void copyGroup(FitsGroup srcGroup, FitsGroup pgroup) throws Exception {
        throw new UnsupportedOperationException("Unsupported copyGroup operation.");
    }

    /**
     * Copies the attributes of one object to another object.
     *
     * FITS does not support attributes
     *
     * @param src
     *            The source object.
     * @param dst
     *            The destination object.
     *
     * @see #copyAttributes(long, long)
     */
    public void copyAttributes(HObject src, HObject dst) {
        throw new UnsupportedOperationException("Unsupported copyAttributes operation.");
    }

    /**
     * Copies the attributes of one object to another object.
     *
     * FITS does not support attributes
     *
     * @param srcID
     *            The source identifier.
     * @param dstID
     *            The destination identifier.
     *
     * @see #copyAttributes(long, long)
     */
    public void copyAttributes(long srcID, long dstID) {
        throw new UnsupportedOperationException("Unsupported copyAttributes with id operation.");
    }

    /**
     * Creates a new attribute and attached to the object if attribute does
     * not exist. Otherwise, just update the value of the attribute.
     *
     * @param obj
     *        the object which the attribute is to be attached to.
     * @param attr
     *        the atribute to attach.
     * @param attrExisted
     *        The indicator if the given attribute exists.
     */
    @Override
    public void writeAttribute(HObject obj, hdf.object.Attribute attr, boolean attrExisted) throws Exception {
        throw new UnsupportedOperationException("Unsupported operation.");
    }

    /**
     *  Returns the version of the library.
     */
    @Override
    public String getLibversion() {
        String ver = "Fits Java (version 2.4)";

        return ver;
    }

    // implementing FileFormat
    @Override
    public HObject get(String path) throws Exception {
        throw new UnsupportedOperationException("get() is not supported");
    }
}

