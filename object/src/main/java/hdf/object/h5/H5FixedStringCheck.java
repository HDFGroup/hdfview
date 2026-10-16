/*****************************************************************************
 * Copyright by The HDF Group.                                               *
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

import java.nio.charset.StandardCharsets;
import java.util.List;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marks any fixed-length string the HDF5 library returned longer than the field it was read
 * from.
 *
 * A fixed-length string only carries a terminator when its field is longer than the text.
 * Since 1.14, the HDF5 library reads until it finds one, which runs past the element
 * whenever the text fills the field.
 * The characters it picks up belong to whatever follows, which may be the
 * next element of the same sequence or the next member of the same compound.
 *
 * The value cannot be recovered on HDFView's side. The conversion from bytes has already happened by the time
 * HDFView sees the string, so each string that overran is replaced with an error marker to prevent showing
 * potentially incorrect data.
 *
 * See HDFGroup/hdfview#504.
 */
class H5FixedStringCheck {

    private static final Logger log = LoggerFactory.getLogger(H5FixedStringCheck.class);

    private static final String ERROR_MARKER = "*ERR:HDFView#504*";

    private H5FixedStringCheck() {}

    /**
     * Mark every fixed-length string in a buffer returned by H5DreadVL that came back longer
     * than its field.
     *
     * @param tid
     *            the datatype the read was made with
     * @param buf
     *            one entry per selected point
     * @param objName
     *            the dataset or attribute name, for the log
     */
    static void markOverrunStrings(long tid, Object[] buf, String objName)
    {
        if (buf == null)
            return;

        int marked = 0;

        try {
            /*
             * Nothing can have overrun unless the datatype holds a fixed-length string
             * somewhere.
             */
            if (!holdsFixedString(tid))
                return;

            for (Object point : buf)
                marked += walk(tid, point);
        }
        catch (Exception ex) {
            log.debug("markOverrunStrings(): giving up on {}", objName, ex);
        }

        if (marked > 0)
            log.warn("{} holds {} fixed-length string(s) the HDF5 library read past the end of "
                         + "their field. Potentially erroneous data was replaced with {}.",
                     objName, marked, ERROR_MARKER);
    }

    /* Returns how many strings were marked at or below data. */
    @SuppressWarnings("unchecked")
    private static int walk(long tid, Object data) throws Exception
    {
        if (!(data instanceof List))
            return 0;

        List<Object> values = (List<Object>)data;
        int typeClass       = H5.H5Tget_class(tid);
        int marked          = 0;

        if (typeClass == HDF5Constants.H5T_COMPOUND) {
            /*
             * Bounded by the list as well, because a member the library dropped
             * leaves it shorter than the datatype says.
             */
            int nmembers = Math.min(H5.H5Tget_nmembers(tid), values.size());

            for (int i = 0; i < nmembers; i++) {
                long memberTid = H5.H5Tget_member_type(tid, i);
                try {
                    marked += markOrWalk(memberTid, values, i);
                }
                finally {
                    H5.H5Tclose(memberTid);
                }
            }
        }
        else if (typeClass == HDF5Constants.H5T_ARRAY || typeClass == HDF5Constants.H5T_VLEN) {
            long baseTid = H5.H5Tget_super(tid);
            try {
                /* Every element shares the base type, so size its field once for all of them. */
                long fieldSize = fixedStringSize(baseTid);

                for (int i = 0; i < values.size(); i++)
                    marked += (fieldSize > 0) ? mark(values, i, fieldSize) : walk(baseTid, values.get(i));
            }
            finally {
                H5.H5Tclose(baseTid);
            }
        }

        return marked;
    }

    /* Mark one value in place if its string overran, otherwise walk into it. */
    private static int markOrWalk(long tid, List<Object> values, int index) throws Exception
    {
        long fieldSize = fixedStringSize(tid);
        return (fieldSize > 0) ? mark(values, index, fieldSize) : walk(tid, values.get(index));
    }

    /*
     * Replace one value when it is a string longer than fieldSize. Compares bytes rather
     * than characters, because the field is sized in bytes and the conversion may have
     * turned bytes picked up beyond it into other characters.
     */
    private static int mark(List<Object> values, int index, long fieldSize)
    {
        if (!(values.get(index) instanceof String str))
            return 0;

        int valueSize = str.getBytes(StandardCharsets.ISO_8859_1).length;
        if (valueSize <= fieldSize)
            return 0;

        log.debug("mark(): a {} byte field came back with {} bytes", fieldSize, valueSize);
        values.set(index, ERROR_MARKER);
        return 1;
    }

    /* True when tid is, or holds at any depth, a fixed-length string. */
    private static boolean holdsFixedString(long tid) throws Exception
    {
        int typeClass = H5.H5Tget_class(tid);

        if (typeClass == HDF5Constants.H5T_STRING)
            return !H5.H5Tis_variable_str(tid);

        if (typeClass == HDF5Constants.H5T_COMPOUND) {
            int nmembers = H5.H5Tget_nmembers(tid);

            for (int i = 0; i < nmembers; i++) {
                long memberTid = H5.H5Tget_member_type(tid, i);
                try {
                    if (holdsFixedString(memberTid))
                        return true;
                }
                finally {
                    H5.H5Tclose(memberTid);
                }
            }
            return false;
        }

        if (typeClass == HDF5Constants.H5T_ARRAY || typeClass == HDF5Constants.H5T_VLEN) {
            long baseTid = H5.H5Tget_super(tid);
            try {
                return holdsFixedString(baseTid);
            }
            finally {
                H5.H5Tclose(baseTid);
            }
        }

        return false;
    }

    /* The field size of a fixed-length string type, or 0 for anything else. */
    private static long fixedStringSize(long tid) throws Exception
    {
        if (H5.H5Tget_class(tid) != HDF5Constants.H5T_STRING || H5.H5Tis_variable_str(tid))
            return 0;

        return H5.H5Tget_size(tid);
    }
}
