/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see https://support.hdfgroup.org/products/licenses.html               *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.object.h5;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5Exception;

public final class H5Utils {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H5Utils.class);

    /**
     * Set up a hyperslab selection within a dataset.
     *
     * @param did
     *            IN dataset ID
     * @param dsetDims
     *            IN dimensions
     * @param startDims
     *            IN start dimensions
     * @param selectedStride
     *            IN selected stride values
     * @param selectedDims
     *            IN selected dimensions
     * @param spaceIDs
     *            IN/OUT memory and file space IDs -- spaceIDs[0]=mspace, spaceIDs[1]=fspace
     *
     * @return total number of data points selected
     *
     * @throws HDF5Exception
     *             If there is an error at the HDF5 library level.
     */
    public static final long selectHyperslab(long did, long[] dsetDims, long[] startDims, long[] selectedStride,
            long[] selectedDims, long[] spaceIDs) throws HDF5Exception {
        log.trace("selectHyperslab(): start");

        if (dsetDims == null) {
            log.debug("selectHyperslab(): dsetDims is null");
            return -1;
        }

        int rank = dsetDims.length;
        if ((startDims != null) && (startDims.length != rank)) {
            log.debug("selectHyperslab(): startDims rank didn't match dsetDims rank");
            return -1;
        }
        if ((selectedStride != null) && (selectedStride.length != rank)) {
            log.debug("selectHyperslab(): selectedStride rank didn't match startDims rank");
            return -1;
        }
        if ((selectedDims != null) && (selectedDims.length != rank)) {
            log.debug("selectHyperslab(): selectedDims rank didn't match startDims rank");
            return -1;
        }

        long lsize = 1;

        boolean isAllSelected = true;
        for (int i = 0; i < rank; i++) {
            if (selectedDims != null) {
                lsize *= selectedDims[i];
                if (selectedDims[i] < dsetDims[i]) {
                    isAllSelected = false;
                }
            }
        }

        log.trace("selectHyperslab(): isAllSelected={}", isAllSelected);

        if (isAllSelected) {
            spaceIDs[0] = HDF5Constants.H5S_ALL;
            spaceIDs[1] = HDF5Constants.H5S_ALL;
        }
        else {
            spaceIDs[1] = H5.H5Dget_space(did);

            // When a 1D dataspace is used for a chunked dataset, reading is very slow.
            //
            // It is a known problem within the HDF5 library.
            // mspace = H5.H5Screate_simple(1, lsize, null);
            spaceIDs[0] = H5.H5Screate_simple(rank, selectedDims, null);
            H5.H5Sselect_hyperslab(spaceIDs[1], HDF5Constants.H5S_SELECT_SET, startDims, selectedStride, selectedDims, null);
        }

        log.trace("selectHyperslab(): finish");

        return lsize;
    }

    public static final long getTotalSelectedSpacePoints(long did, long[] dsetDims, long[] startDims,
            long[] selectedStride, long[] selectedDims, long[] spaceIDs) throws HDF5Exception {
        long totalSelectedSpacePoints = selectHyperslab(did, dsetDims, startDims, selectedStride, selectedDims, spaceIDs);

        log.trace("getTotalSelectedSpacePoints(): selected {} points in dataset's dataspace", totalSelectedSpacePoints);

        if (totalSelectedSpacePoints == 0) {
            log.debug("getTotalSelectedSpacePoints(): No data to read. Dataset or selected subset is empty.");
            log.trace("getTotalSelectedSpacePoints(): finish");
            throw new HDF5Exception("No data to read.\nEither the dataset or the selected subset is empty.");
        }

        if (totalSelectedSpacePoints < Integer.MIN_VALUE || totalSelectedSpacePoints > Integer.MAX_VALUE) {
            log.debug("getTotalSelectedSpacePoints(): totalSelectedSpacePoints outside valid Java int range; unsafe cast");
            log.trace("getTotalSelectedSpacePoints(): finish");
            throw new HDF5Exception("Invalid int size");
        }

        if (log.isDebugEnabled()) {
            // check is storage space is allocated
            try {
                long ssize = H5.H5Dget_storage_size(did);
                log.trace("getTotalSelectedSpacePoints(): Storage space allocated = {} bytes", ssize);
            }
            catch (Exception ex) {
                log.debug("getTotalSelectedSpacePoints(): check if storage space is allocated:", ex);
            }
        }

        return totalSelectedSpacePoints;
    }

}
