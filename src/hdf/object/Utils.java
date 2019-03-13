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

package hdf.object;

public final class Utils {
    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieves the Java Runtime Class of the given Object. B = byte array, S = short array, I = int
     * array, J = long array, F = float array, D = double array, L = class or interface
     *
     * @param o
     *            the Object to determine the Runtime Class of
     * @return the Java Runtime Class of the given Object.
     */
    public static char getJavaObjectRuntimeClass(Object o) {
        if (o == null)
            return ' ';

        String cName = o.getClass().getName();

        if (cName.equals("java.lang.String") || cName.equals("java.util.Vector")
                || cName.equals("java.util.Arrays$ArrayList") || cName.equals("java.util.ArrayList"))
            return 'L';

        int cIndex = cName.lastIndexOf('[');
        if (cIndex >= 0) {
            return cName.charAt(cIndex + 1);
        }

        return ' ';
    }

}