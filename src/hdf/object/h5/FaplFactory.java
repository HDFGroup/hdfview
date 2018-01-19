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

package hdf.object.h5;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5LibraryException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Go-to class for File Access Property List manufacture
 *
 * Provides utilities to: create a default file access property list; close
 * any property list; manage subclassed types which detail the creation of
 * specific FAPL types, e.g. Read-Only S3 or REST-VOL
 *
 * @author jake.smith
 */
public abstract class FaplFactory {

    private static Map<String, FaplFactory> FaplTypes = new HashMap<>();

    /**
     * Wrapper to H5Pcreate(H5P_FILE_ACCESS)
     *
     * @return fapl_id of created entity
     * @throws HDF5LibraryException as raised by libhdf5 upon error
     */
    public static long createDefaultFapl() throws HDF5LibraryException {
        return H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
    }

    /**
     * Wrapper for H5Pclose(long)
     *
     * @param fapl_id identifier to fapl entity.
     * @throws HDF5LibraryException as raised by libhdf5; most likely fapl_id
     *         is already closed or not an id of a FAPL instance.
     */
    public static void closeFapl(long fapl_id) throws HDF5LibraryException {
        H5.H5Pclose(fapl_id);
    }

    /**
     * Add a FaplFactory subclass to available factories.
     * If the given key is already in use, will replace the existing Factory
     * with the one provided.
     *
     * @param key Identifier of the Fapl type, e.g. "ros3". Need not correspond
     *            in any way with the underlying mechanism, but better to be
     *            clear.
     * @param ff Class instance of FaplFactory subclass.
     * @throws IllegalArgumentException raised if ff is null.
     */
    public static void registerFaplType(String key, FaplFactory ff)
    throws IllegalArgumentException
    {
        if (ff == null)
            throw new IllegalArgumentException("Fapl Factory cannot be null");
        FaplTypes.put(key, ff);
    }

    /**
     * Remove FaplFactory subclass from available factories.
     *
     * @param key Identifier of the Fapl type.
     * @throws IllegalArgumentException Raised iff key is not registered.
     */
    public static void unregisterFaplType(String key)
    throws IllegalArgumentException
    {
        if (!FaplTypes.containsKey(key))
            throw new IllegalArgumentException(
                    "attempting to unregister an absent Fapl type");
        FaplTypes.remove(key);
    }

    /**
     * Get set of all registered FaplFactory types by key
     */
    public static Set<String> getTypes() {
        return FaplTypes.keySet();
    }

    /**
     * Get the FaplFactory instance associated with the given key.
     *
     * @param key Identifier for FaplFactory type. Must be found in registered
     *            keys.
     * @throws IllegalArgumentException If key/factory is not registered.
     */
    public static FaplFactory getFactory(String key) {
        if (!FaplTypes.containsKey(key))
            throw new IllegalArgumentException(
                    "attempting to get an unregistered Fapl type");
        return FaplTypes.get(key);
    }

    /**
     * Create a new fapl within the HDF5 library, according to the details of
     * the implementation.
     *
     * @param template all data pertinent to the creation of a fapl of the
     *                 implementing type.
     * @return (long)fapl_id of the new fapl entity.
     * @throws Exception thrown as relevant by implementing classes.
     */
    public abstract long createFapl(Map<String, String> template) throws Exception;
}
