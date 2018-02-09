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
import hdf.hdf5lib.structs.H5FD_ros3_fapl_t;

import java.util.Map;

/**
 * @author jake.smith
 */
public class FaplFactoryROS3 extends FaplFactory {

    @Override
    public long createFapl(Map<String, String> template)
            throws HDF5LibraryException {
        long fapl_id = H5.H5Pcreate(HDF5Constants.H5P_FILE_ACCESS);
        H5FD_ros3_fapl_t fa = new H5FD_ros3_fapl_t(
                template.get("aws-region"),
                template.get("secret-id"),
                template.get("secret-key"));
        H5.H5Pset_fapl_ros3(fapl_id, fa);

        return fapl_id;
    }
}
