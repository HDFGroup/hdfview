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

import hdf.object.Attribute;
import hdf.object.h5.H5File;

/**
 * An interface that provides general attribute operations for hdf5 object data. For
 * example, reference to a parent object.
 *
 * @see hdf.object.HObject
 */
public interface H5Attribute extends Attribute {

    /**
     * The general read and write attribute operations for hdf5 object data.
     *
     * @param attr_id
     *        the attribute to access
     * @param ioType
     *        the type of IO operation
     * @param objBuf
     *        the data buffer to use for write operation
     *
     * @return the attribute data
     *
     * @throws Exception
     *             if the data can not be retrieved
     */
    Object AttributeCommonIO(long attr_id, H5File.IO_TYPE ioType, Object objBuf) throws Exception;

    /**
     * Read a subset of an attribute for hdf5 object data.
     *
     * @return the selected attribute data
     *
     * @throws Exception
     *             if the data can not be retrieved
     */
    Object AttributeSelection() throws Exception;
}
