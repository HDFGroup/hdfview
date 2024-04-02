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

package hdf.object.h5;

import java.util.ArrayList;
import java.util.List;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5Exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HDF5 plugin utility class */
public final class H5Plugins {

    private static final Logger log = LoggerFactory.getLogger(H5Plugins.class);

    private static long totalPaths;

    /**
     * Creates a list of plugin paths for HDFView.
     *
     */
    public H5Plugins()
    {
        totalPaths = H5.H5PLsize(); // initial number of paths
    }

    /**
     * Get the total number of paths for the HDF5 library.
     *
     * @return total number of plugin paths
     *
     * @throws HDF5Exception If there is an error at the HDF5 library level.
     */
    public static final long getTotalPluginPaths() throws HDF5Exception
    {
        totalPaths = H5.H5PLsize();

        log.trace("getTotalPluginPaths(): total plugin paths {}", totalPaths);

        return totalPaths;
    }

    /**
     * Get the list of paths for the HDF5 library.
     *
     * @return list of plugin paths
     *
     * @throws HDF5Exception If there is an error at the HDF5 library level.
     */
    public static final ArrayList<String> getPluginPaths() throws HDF5Exception
    {
        ArrayList<String> pathList = new ArrayList<>(5);
        totalPaths                 = H5.H5PLsize();
        log.trace("getPluginPaths(): total plugin paths {}", totalPaths);
        for (int indx = 0; indx < totalPaths; indx++)
            pathList.add(H5.H5PLget(indx));

        return pathList;
    }

    /**
     * Replaces the plugin path.
     *
     * @param pluginPath The plugin path.
     * @param pathIndex  The index to replace the plugin path.
     */
    public static void replacePluginPath(String pluginPath, int pathIndex) throws HDF5Exception
    {
        H5.H5PLreplace(pluginPath, pathIndex);
    }

    /**
     * Inserts the plugin path.
     *
     * @param pluginPath The plugin path.
     * @param pathIndex  The index to insert the plugin path.
     */
    public static void insertPluginPath(String pluginPath, int pathIndex) throws HDF5Exception
    {
        H5.H5PLinsert(pluginPath, pathIndex);
    }

    /**
     * Removes the plugin path.
     *
     * @param pathIndex  The index to remove the plugin path.
     */
    public static void deletePluginPath(int pathIndex) throws HDF5Exception { H5.H5PLremove(pathIndex); }

    /**
     * Prepend the plugin path.
     *
     * @param pluginPath The plugin path.
     */
    public static void prependPluginPath(String pluginPath) throws HDF5Exception
    {
        H5.H5PLprepend(pluginPath);
    }

    /**
     * Append the plugin path.
     *
     * @param pluginPath The plugin path.
     */
    public static void appendPluginPath(String pluginPath) throws HDF5Exception { H5.H5PLappend(pluginPath); }
}
