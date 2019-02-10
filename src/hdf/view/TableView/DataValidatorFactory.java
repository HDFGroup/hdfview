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

package hdf.view.TableView;

import java.math.BigInteger;
import java.util.StringTokenizer;

import org.eclipse.nebula.widgets.nattable.data.validate.DataValidator;
import org.eclipse.nebula.widgets.nattable.data.validate.ValidationFailedException;

import hdf.object.CompoundDS;
import hdf.object.Datatype;

/**
 * A Factory class to return a DataValidator class for a NatTable instance based
 * upon the Datatype that it is supplied.
 *
 * @author Jordan T. Henderson
 * @version 1.0 6/28/2018
 *
 */
public class DataValidatorFactory {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataValidatorFactory.class);

    public static DataValidator getDataValidator(Datatype dtype) throws Exception {
        if (dtype == null)
            throw new Exception("DataValidatorFactory supplied with an invalid Datatype");

        DataValidator validator = null;

        log.trace("getDataValidator(Datatype): start");

        try {
            if (dtype.isCompound())
                throw new Exception("Incorrect method getDataValidator(Datatype) called for CompoundDS; use getDataValidator(CompoundDS) instead");

            if (dtype.isArray())
                validator = new ArrayDataValidator(dtype);
            else if (dtype.isInteger() || dtype.isFloat())
                validator = new NumericalDataValidator(dtype);
            else if (dtype.isVLEN() && !dtype.isVarStr())
                validator = new VlenDataValidator(dtype);
            else if (dtype.isString())
                validator = new StringDataValidator(dtype);
        }
        catch (Exception ex) {
            log.debug("getDataValidator(Datatype): error occurred in retrieving a DataValidator: ", ex);
            validator = null;
        }

        /*
         * By default, never validate if a proper DataValidator was not found.
         */
        if (validator == null) {
            log.debug("getDataValidator(Datatype): using a default data validator");

            validator = new DataValidator() {
                @Override
                public boolean validate(int arg0, int arg1, Object arg2) {
                    throw new ValidationFailedException("A proper DataValidator wasn't found for this type of data. Writing this type of data will be disabled.");
                }
            };
        }

        log.trace("getDataValidator(Datatype): finish");

        return validator;
    }

    public static DataValidator getDataValidator(CompoundDS compoundDataset) throws Exception {
        if (compoundDataset == null)
            throw new Exception("Must supply a valid CompoundDS to the DataValidatorFactory");

        DataValidator validator = null;

        log.trace("getDataValidator(CompoundDS): start");

        try {
            validator = new CompoundDataValidator(compoundDataset);
        }
        catch (Exception ex) {
            log.debug("getDataValidator(CompoundDS): error occurred in retrieving a DataValidator: ", ex);
            validator = null;
        }

        /*
         * By default, never validate if a proper DataValidator was not found.
         */
        if (validator == null) {
            log.debug("getDataValidator(CompoundDS): using a default data validator");

            validator = new DataValidator() {
                @Override
                public boolean validate(int arg0, int arg1, Object arg2) {
                    throw new ValidationFailedException("A proper DataValidator wasn't found for this type of data. Writing this type of data will be disabled.");
                }
            };
        }

        log.trace("getDataValidator(CompoundDS): finish");

        return validator;
    }

    /*
     * NatTable DataValidator to validate entered input for a dataset with
     * a Compound datatype by calling the appropriate validator on the member
     * at the given row and column index. The correct validator is determined
     * by taking the column index modulo the number of selected members in the
     * Compound datatype, and grabbing the correct validator from the stored
     * list of validators.
     */
    private static class CompoundDataValidator extends DataValidator {

        private final DataValidator[] memberValidators;
        private final int numSelectedMembers;

        CompoundDataValidator(CompoundDS dset) throws Exception {
            if (dset == null)
                throw new Exception("CompoundDataValidator supplied with an invalid CompoundDS");

            Datatype dtype = dset.getDatatype();
            if (dtype == null)
                throw new Exception("CompoundDataValidator's CompoundDS has an invalid Datatype");

            log.trace("CompoundDataValidator: Datatype is {}", dtype.getDescription());

            Datatype[] selectedMembers = dset.getSelectedMemberTypes();
            numSelectedMembers = selectedMembers.length;
            memberValidators = new DataValidator[numSelectedMembers];

            log.trace("CompoundDataValidator: number of selected members {}", numSelectedMembers);

            for (int i = 0; i < numSelectedMembers; i++) {
                memberValidators[i] = DataValidatorFactory.getDataValidator(selectedMembers[i]);
            }
        }

        @Override
        public boolean validate(int colIndex, int rowIndex, Object newValue) {
            int fieldIdx = colIndex % numSelectedMembers;
            if (!memberValidators[fieldIdx].validate(colIndex, rowIndex, newValue))
                return false;

            return true;
        }
    }

    /*
     * NatTable DataValidator to validate entered input for a dataset with
     * an ARRAY datatype by calling the appropriate validator (as determined
     * by the supplied datatype) on each of the array's elements.
     */
    private static class ArrayDataValidator extends DataValidator {

        private final DataValidator baseValidator;

        ArrayDataValidator(Datatype dtype) throws Exception {
            if (dtype == null || !dtype.isArray())
                throw new Exception("ArrayDataValidator supplied with an invalid array Datatype");

            Datatype baseType = dtype.getDatatypeBase();
            if (baseType == null)
                throw new Exception("ArrayDataValidator's Array Datatype has an invalid base Datatype");

            log.trace("ArrayDataValidator: base Datatype is {}", baseType.getDescription());

            if (baseType.isArray())
                this.baseValidator = new ArrayDataValidator(baseType);
            else if (baseType.isInteger() || baseType.isFloat())
                this.baseValidator = new NumericalDataValidator(baseType);
            else if (baseType.isVLEN() && !baseType.isVarStr())
                this.baseValidator = new VlenDataValidator(baseType);
            else if (baseType.isString())
                this.baseValidator = new StringDataValidator(baseType);
            else
                throw new Exception("Unable to find a suitable base validator class for this ArrayDataValidator");
        }

        @Override
        public boolean validate(int colIndex, int rowIndex, Object newValue) {
            if (!(newValue instanceof String))
                throw new ValidationFailedException("Cannot validate Array data input: data is not a String");

            StringTokenizer elementReader = new StringTokenizer((String) newValue, " \t\n\r\f,");
            while (elementReader.hasMoreTokens()) {
                String nextToken = elementReader.nextToken();
                if (!baseValidator.validate(colIndex, rowIndex, nextToken))
                    return false;
            }

            return true;
        }
    }

    /*
     * NatTable DataValidator to validate entered input for a dataset with
     * a variable-length Datatype (note that this DataValidator should not
     * be used for String Datatypes that are variable-length).
     */
    private static class VlenDataValidator extends DataValidator {

        private final DataValidator baseValidator;

        VlenDataValidator(Datatype dtype) throws Exception {
            if (dtype == null || !dtype.isVLEN())
                throw new Exception("VlenDataValidator supplied with an invalid valid VLEN Datatype");

            Datatype baseType = dtype.getDatatypeBase();
            if (baseType == null)
                throw new Exception("VlenDataValidator's VLEN Datatype has an invalid base Datatype");

            log.trace("VlenDataValidator: base Datatype is {}", baseType.getDescription());

            if (baseType.isArray())
                this.baseValidator = new ArrayDataValidator(baseType);
            else if (baseType.isInteger() || baseType.isFloat())
                this.baseValidator = new NumericalDataValidator(baseType);
            else if (baseType.isVLEN() && !baseType.isVarStr())
                this.baseValidator = new VlenDataValidator(baseType);
            else if (baseType.isString())
                this.baseValidator = new StringDataValidator(baseType);
            else
                throw new Exception("Unable to find a suitable base validator class for this VlenDataValidator");
        }

        @Override
        public boolean validate(int colIndex, int rowIndex, Object newValue) {
            if (!(newValue instanceof String))
                throw new ValidationFailedException("Cannot validate data input: data is not a String");

            StringTokenizer elementReader = new StringTokenizer((String) newValue, " \t\n\r\f,()");
            while (elementReader.hasMoreTokens()) {
                String nextToken = elementReader.nextToken();
                if (!baseValidator.validate(colIndex, rowIndex, nextToken))
                    return false;
            }

            return true;
        }
    }

    /*
     * NatTable DataValidator to validate entered input for a dataset with
     * a numerical Datatype.
     */
    private static class NumericalDataValidator extends DataValidator {

        private final Datatype datasetDatatype;

        NumericalDataValidator(Datatype dtype) throws Exception {
            if (dtype == null || (!dtype.isInteger() && !dtype.isFloat()))
                throw new Exception("NumericalDataValidator supplied with an invalid numerical Datatype");

            log.trace("NumericalDataValidator: base Datatype is {}", dtype.getDescription());

            this.datasetDatatype = dtype;
        }

        @Override
        public boolean validate(int colIndex, int rowIndex, Object newValue) {
            if (!(newValue instanceof String))
                throw new ValidationFailedException("Cannot validate numerical data input: data is not a String");

            try {
                switch ((int) datasetDatatype.getDatatypeSize()) {
                    case 1:
                        if (datasetDatatype.isUnsigned()) {
                            /*
                             * First try to parse as a larger type in order to catch a NumberFormatException
                             */
                            Short shortValue = Short.parseShort((String) newValue);
                            if (shortValue < 0)
                                throw new NumberFormatException("Invalid negative value for unsigned datatype");

                            if (shortValue > (Byte.MAX_VALUE * 2) + 1)
                                throw new NumberFormatException("Value out of range. Value:\"" + newValue + "\"");
                        }
                        else {
                            Byte.parseByte((String) newValue);
                        }
                        break;

                    case 2:
                        if (datasetDatatype.isUnsigned()) {
                            /*
                             * First try to parse as a larger type in order to catch a NumberFormatException
                             */
                            Integer intValue = Integer.parseInt((String) newValue);
                            if (intValue < 0)
                                throw new NumberFormatException("Invalid negative value for unsigned datatype");

                            if (intValue > (Short.MAX_VALUE * 2) + 1)
                                throw new NumberFormatException("Value out of range. Value:\"" + newValue + "\"");
                        }
                        else {
                            Short.parseShort((String) newValue);
                        }
                        break;

                    case 4:
                        if (datasetDatatype.isInteger()) {
                            if (datasetDatatype.isUnsigned()) {
                                /*
                                 * First try to parse as a larger type in order to catch a NumberFormatException
                                 */
                                Long longValue = Long.parseLong((String) newValue);
                                if (longValue < 0)
                                    throw new NumberFormatException("Invalid negative value for unsigned datatype");

                                if (longValue > ((long) Integer.MAX_VALUE * 2) + 1)
                                    throw new NumberFormatException("Value out of range. Value:\"" + newValue + "\"");
                            }
                            else {
                                Integer.parseInt((String) newValue, 10);
                            }
                        }
                        else {
                            /* Floating-point type */
                            Float.parseFloat((String) newValue);
                        }
                        break;

                    case 8:
                        if (datasetDatatype.isInteger()) {
                            if (datasetDatatype.isUnsigned()) {
                                /*
                                 * First try to parse as a larger type in order to catch a NumberFormatException
                                 */
                                BigInteger bigValue = new BigInteger((String) newValue);
                                if (bigValue.compareTo(BigInteger.ZERO) < 0)
                                    throw new NumberFormatException("Invalid negative value for unsigned datatype");

                                BigInteger maxRange = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(2)).add(BigInteger.valueOf(1));
                                if (bigValue.compareTo(maxRange) > 0)
                                    throw new NumberFormatException("Value out of range. Value:\"" + newValue + "\"");
                            }
                            else {
                                Long.parseLong((String) newValue);
                            }
                        }
                        else {
                            /* Floating-point type */
                            Double.parseDouble((String) newValue);
                        }
                        break;

                    default:
                        throw new Exception("No validation logic for numerical data of size " + datasetDatatype.getDatatypeSize());
                }
            }
            catch (Exception ex) {
                throw new ValidationFailedException("Failed to update value at " + "(" + rowIndex + ", "
                        + colIndex + ") to '" + newValue.toString() + "': " + ex.toString());
            }

            return true;
        }
    }

    /*
     * NatTable DataValidator to validate entered input for a dataset with
     * a String Datatype (including Strings of variable-length).
     */
    private static class StringDataValidator extends DataValidator {

        private final Datatype datasetDatatype;

        StringDataValidator(Datatype dtype) throws Exception {
            if (dtype == null || !dtype.isString())
                throw new Exception("StringDataValidator supplied with an invalid String Datatype");

            log.trace("StringDataValidator: base Datatype is {}", dtype.getDescription());

            this.datasetDatatype = dtype;
        }

        @Override
        public boolean validate(int colIndex, int rowIndex, Object newValue) {
            if (!(newValue instanceof String))
                throw new ValidationFailedException("Cannot validate string data input: data is not a String");

            try {
                /*
                 * If this is a fixed-length string type, check to make sure that the data
                 * length does not exceed the datatype size.
                 */
                /*
                 * TODO: Add warning about overwriting NULL-terminator for NULLTERM type strings
                 */
                if (!datasetDatatype.isVarStr()) {
                    long lenDiff = ((String) newValue).length() - datasetDatatype.getDatatypeSize();

                    if (lenDiff > 0)
                        throw new Exception("string size larger than datatype size by " + lenDiff + ((lenDiff > 1) ? " bytes." : " byte."));
                }
            }
            catch (Exception ex) {
                throw new ValidationFailedException("Failed to update value at " + "(" + rowIndex + ", "
                        + colIndex + ") to '" + newValue.toString() + "': " + ex.getMessage());
            }

            return true;
        }
    }

}
