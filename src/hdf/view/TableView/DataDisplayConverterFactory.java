/*****************************************************************************
 * Copyright by The HDF Group.                                               *
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

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.text.NumberFormat;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;

import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.object.CompoundDataFormat;
import hdf.object.DataFormat;
import hdf.object.Datatype;
import hdf.object.h5.H5Datatype;
import hdf.view.Tools;

/**
 * A Factory class to return a concrete class implementing the IDisplayConverter
 * interface in order to convert data values into human-readable forms in a NatTable.
 * The returned class is also responsible for converting the human-readable form back
 * into real data when writing the data object back to the file.
 *
 * @author Jordan T. Henderson
 * @version 1.0 2/9/2019
 *
 */
public class DataDisplayConverterFactory {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataDisplayConverterFactory.class);

    /*
     * To keep things clean from an API perspective, keep a static reference to the last
     * CompoundDataFormat that was passed in. This keeps us from needing to pass the
     * CompoundDataFormat object as a parameter to every DataDisplayConverter class,
     * since it's really only needed by the CompoundDataDisplayConverter.
     */
    private static CompoundDataFormat compoundFormatReference = null;

    public static HDFDisplayConverter getDataDisplayConverter(final DataFormat dataObject) throws Exception {
        log.trace("getDataDisplayConverter(DataFormat): start");

        if (dataObject == null) {
            log.debug("getDataDisplayConverter(DataFormat): data object is null");
            return null;
        }

        if (dataObject instanceof CompoundDataFormat)
            compoundFormatReference = (CompoundDataFormat) dataObject;

        HDFDisplayConverter converter = getDataDisplayConverter(dataObject.getDatatype());

        log.trace("getDataDisplayConverter(DataFormat): finish");

        return converter;
    }

    private static final HDFDisplayConverter getDataDisplayConverter(final Datatype dtype) throws Exception {
        log.trace("getDataDisplayConverter(Datatype): start");

        HDFDisplayConverter converter = null;

        try {
            if (dtype.isCompound())
                converter = new CompoundDataDisplayConverter(dtype);
            else if (dtype.isArray())
                converter = new ArrayDataDisplayConverter(dtype);
            else if (dtype.isVLEN() && !dtype.isVarStr())
                converter = new VlenDataDisplayConverter(dtype);
            else if (dtype.isString())
                converter = new StringDataDisplayConverter(dtype);
            else if (dtype.isChar())
                converter = new CharDataDisplayConverter(dtype);
            else if (dtype.isInteger() || dtype.isFloat())
                converter = new NumericalDataDisplayConverter(dtype);
            else if (dtype.isEnum())
                converter = new EnumDataDisplayConverter(dtype);
            else if (dtype.isOpaque() || dtype.isBitField())
                converter = new BitfieldDataDisplayConverter(dtype);
            else if (dtype.isRef())
                converter = new RefDataDisplayConverter(dtype);
        }
        catch (Exception ex) {
            log.debug("getDataDisplayConverter(Datatype): error occurred in retrieving a DataDisplayConverter: ", ex);
            converter = null;
        }

        /*
         * Try to use a default converter.
         */
        if (converter == null) {
            log.debug("getDataDisplayConverter(Datatype): using a default data display converter");

            converter = new HDFDisplayConverter() {
                @Override
                public Object canonicalToDisplayValue(Object value) {
                    return value;
                }

                @Override
                public Object displayToCanonicalValue(Object value) {
                    return value;
                }
            };
        }

        log.trace("getDataDisplayConverter(Datatype): finish");

        return converter;
    }

    public static abstract class HDFDisplayConverter extends DisplayConverter {

        protected NumberFormat numberFormat = null;

        protected boolean      showAsHex = false;
        protected boolean      showAsBin = false;
        protected boolean      isEnumConverted = false;

        public void setNumberFormat(NumberFormat format) {
            numberFormat = format;
        }

        public void setShowAsHex(boolean asHex) {
            showAsHex = asHex;
        }

        public void setShowAsBin(boolean asBin) {
            showAsBin = asBin;
        }

        public void setConvertEnum(boolean convert) {
            isEnumConverted = convert;
        }

    }

    private static class CompoundDataDisplayConverter extends HDFDisplayConverter {

        private final HDFDisplayConverter[] memberTypeConverters;

        private final StringBuffer          buffer;

        public final int                    nFields;

        private int                         fieldIdx;

        CompoundDataDisplayConverter(final Datatype dtype) throws Exception {
            log.trace("CompoundDataDisplayConverter: start");

            if (!dtype.isCompound()) {
                log.debug("CompoundDataDisplayConverter: datatype is not a compound type");
                throw new Exception("CompoundDataDisplayConverter: datatype is not a compound type");
            }

            Datatype[] memberTypes = compoundFormatReference.getSelectedMemberTypes();
            if (memberTypes == null) {
                log.debug("CompoundDataDisplayConverter: compound member datatype list is null");
                throw new Exception("CompoundDataDisplayConverter: compound member datatype list is null");
            }

            memberTypeConverters = new HDFDisplayConverter[memberTypes.length];

            for (int i = 0; i < memberTypes.length; i++) {
                Datatype memberType = memberTypes[i];

                log.trace("CompoundDataDisplayConverter: retrieving DataDisplayConverter for member {}", i);

                try {
                    memberTypeConverters[i] = getDataDisplayConverter(memberType);

                    /*
                     * Make base datatype converters inherit the data conversion settings.
                     */
                    memberTypeConverters[i].setShowAsHex(this.showAsHex);
                    memberTypeConverters[i].setShowAsBin(this.showAsBin);
                    memberTypeConverters[i].setNumberFormat(this.numberFormat);
                    memberTypeConverters[i].setConvertEnum(this.isEnumConverted);
                }
                catch (Exception ex) {
                    log.debug("CompoundDataDisplayConverter: failed to retrieve DataDisplayConverter for member {}: ", i, ex);
                    memberTypeConverters[i] = null;
                }
            }

            nFields = memberTypes.length;

            buffer = new StringBuffer();

            log.trace("CompoundDataDisplayConverter: finish");
        }

        @Override
        public Object canonicalToDisplayValue(ILayerCell cell, IConfigRegistry configRegistry, Object value) {
            fieldIdx = cell.getColumnIndex() % nFields;
            return canonicalToDisplayValue(value);
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("CompoundDataDisplayConverter: canonicalToDisplayValue({}): start", value);

            buffer.setLength(0); // clear the old string

            try {
                Object convertedValue = memberTypeConverters[fieldIdx].canonicalToDisplayValue(value);
                buffer.append(convertedValue);
            }
            catch (Exception ex) {
                log.debug("CompoundDataDisplayConverter: canonicalToDisplayValue() failure: ", ex);
                buffer.append("*ERROR*");
            }

            log.trace("CompoundDataDisplayConverter: canonicalToDisplayValue({}): finish", buffer);

            return buffer;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            return value;
        }

        @Override
        public void setNumberFormat(NumberFormat format) {
            super.setNumberFormat(format);

            for (int i = 0; i < memberTypeConverters.length; i++) {
                memberTypeConverters[i].setNumberFormat(format);
            }
        }

        @Override
        public void setShowAsHex(boolean asHex) {
            super.setShowAsHex(asHex);

            for (int i = 0; i < memberTypeConverters.length; i++) {
                memberTypeConverters[i].setShowAsHex(asHex);
            }
        }

        @Override
        public void setShowAsBin(boolean asBin) {
            super.setShowAsBin(asBin);

            for (int i = 0; i < memberTypeConverters.length; i++) {
                memberTypeConverters[i].setShowAsBin(asBin);
            }
        }

        @Override
        public void setConvertEnum(boolean convert) {
            super.setConvertEnum(convert);

            for (int i = 0; i < memberTypeConverters.length; i++) {
                memberTypeConverters[i].setConvertEnum(convert);
            }
        }

    }

    private static class ArrayDataDisplayConverter extends HDFDisplayConverter {

        private final HDFDisplayConverter baseTypeConverter;

        private final StringBuffer        buffer;

        ArrayDataDisplayConverter(final Datatype dtype) throws Exception {
            log.trace("ArrayDataDisplayConverter: start");

            if (!dtype.isArray()) {
                log.debug("ArrayDataDisplayConverter: datatype is not an array type");
                throw new Exception("ArrayDataDisplayConverter: datatype is not an array type");
            }

            Datatype baseType = dtype.getDatatypeBase();

            if (baseType == null) {
                log.debug("ArrayDataDisplayConverter: base datatype is null");
                throw new Exception("ArrayDataDisplayConverter: base datatype is null");
            }

            try {
                baseTypeConverter = getDataDisplayConverter(baseType);

                /*
                 * Make base datatype converter inherit the data conversion settings.
                 */
                baseTypeConverter.setShowAsHex(this.showAsHex);
                baseTypeConverter.setShowAsBin(this.showAsBin);
                baseTypeConverter.setNumberFormat(this.numberFormat);
                baseTypeConverter.setConvertEnum(this.isEnumConverted);
            }
            catch (Exception ex) {
                log.debug("ArrayDataDisplayConverter: couldn't get DataDisplayConverter for base datatype: ", ex);
                throw new Exception("ArrayDataDisplayConverter: couldn't get DataDisplayConverter for base datatype: " + ex.getMessage());
            }

            buffer = new StringBuffer();

            log.trace("ArrayDataDisplayConverter: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("ArrayDataDisplayConverter: canonicalToDisplayValue({}): start", value);

            buffer.setLength(0); // clear the old string

            try {
                Object obj;
                Object convertedValue;
                int arrLen = Array.getLength(value);

                log.trace("ArrayDataDisplayConverter: array length={}", arrLen);

                for (int i = 0; i < arrLen; i++) {
                    if (i > 0) buffer.append(", ");

                    obj = Array.get(value, i);
                    convertedValue = baseTypeConverter.canonicalToDisplayValue(obj);

                    buffer.append(convertedValue);
                }
            }
            catch (Exception ex) {
                log.debug("ArrayDataDisplayConverter: canonicalToDisplayValue() failure: ", ex);
                buffer.append("*ERROR*");
            }

            log.trace("ArrayDataDisplayConverter: canonicalToDisplayValue({}): finish", buffer);

            return buffer;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            return value;
        }

        @Override
        public void setNumberFormat(NumberFormat format) {
            super.setNumberFormat(format);

            baseTypeConverter.setNumberFormat(format);
        }

        @Override
        public void setShowAsHex(boolean asHex) {
            super.setShowAsHex(asHex);

            baseTypeConverter.setShowAsHex(asHex);
        }

        @Override
        public void setShowAsBin(boolean asBin) {
            super.setShowAsBin(asBin);

            baseTypeConverter.setShowAsBin(asBin);
        }

        @Override
        public void setConvertEnum(boolean convert) {
            super.setConvertEnum(convert);

            baseTypeConverter.setConvertEnum(convert);
        }

    }

    private static class VlenDataDisplayConverter extends HDFDisplayConverter {

        private final HDFDisplayConverter baseTypeConverter;

        VlenDataDisplayConverter(final Datatype dtype) throws Exception {
            log.trace("VlenDataDisplayConverter: start");

            if (!dtype.isVLEN()) {
                log.debug("VlenDataDisplayConverter: datatype is not a variable-length type");
                throw new Exception("VlenDataDisplayConverter: datatype is not a variable-length type");
            }

            Datatype baseType = dtype.getDatatypeBase();

            if (baseType == null) {
                log.debug("VlenDataDisplayConverter: base datatype is null");
                throw new Exception("VlenDataDisplayConverter: base datatype is null");
            }

            try {
                baseTypeConverter = getDataDisplayConverter(baseType);

                /*
                 * Make base datatype converter inherit the data conversion settings.
                 */
                baseTypeConverter.setShowAsHex(this.showAsHex);
                baseTypeConverter.setShowAsBin(this.showAsBin);
                baseTypeConverter.setNumberFormat(this.numberFormat);
                baseTypeConverter.setConvertEnum(this.isEnumConverted);
            }
            catch (Exception ex) {
                log.debug("VlenDataDisplayConverter: couldn't get DataDisplayConverter for base datatype: ", ex);
                throw new Exception("VlenDataDisplayConverter: couldn't get DataDisplayConverter for base datatype: " + ex.getMessage());
            }

            log.trace("VlenDataDisplayConverter: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            return value;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            return value;
        }

        @Override
        public void setNumberFormat(NumberFormat format) {
            super.setNumberFormat(format);

            baseTypeConverter.setNumberFormat(format);
        }

        @Override
        public void setShowAsHex(boolean asHex) {
            super.setShowAsHex(asHex);

            baseTypeConverter.setShowAsHex(asHex);
        }

        @Override
        public void setShowAsBin(boolean asBin) {
            super.setShowAsBin(asBin);

            baseTypeConverter.setShowAsBin(asBin);
        }

        @Override
        public void setConvertEnum(boolean convert) {
            super.setConvertEnum(convert);

            baseTypeConverter.setConvertEnum(convert);
        }

    }

    private static class StringDataDisplayConverter extends HDFDisplayConverter {

        StringDataDisplayConverter(final Datatype dtype) throws Exception {
            log.trace("StringDataDisplayConverter: start");

            if (!dtype.isString() && !dtype.isVarStr()) {
                log.debug("StringDataDisplayConverter: datatype is not a string type");
                throw new Exception("StringDataDisplayConverter: datatype is not a string type");
            }

            log.trace("StringDataDisplayConverter: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            return value;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            return value;
        }

    }

    private static class CharDataDisplayConverter extends HDFDisplayConverter {

        CharDataDisplayConverter(final Datatype dtype) throws Exception {
            log.trace("CharDataDisplayConverter: start");

            if (!dtype.isChar()) {
                log.debug("CharDataDisplayConverter: datatype is not a character type");
                throw new Exception("CharDataDisplayConverter: datatype is not a character type");
            }

            log.trace("CharDataDisplayConverter: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            return value;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            return value;
        }

    }

    private static class NumericalDataDisplayConverter extends HDFDisplayConverter {

        private final StringBuffer buffer;

        private final long         typeSize;

        private final boolean      isUINT64;

        NumericalDataDisplayConverter(final Datatype dtype) throws Exception {
            log.trace("NumericalDataDisplayConverter: start");

            if (!dtype.isInteger() && !dtype.isFloat()) {
                log.debug("NumericalDataDisplayConverter: datatype is not an integer or floating-point type");
                throw new Exception("NumericalDataDisplayConverter: datatype is not an integer or floating-point type");
            }

            buffer = new StringBuffer();

            typeSize = dtype.getDatatypeSize();
            isUINT64 = dtype.isUnsigned() && (typeSize == 8);

            log.trace("NumericalDataDisplayConverter: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("NumericalDataDisplayConverter: canonicalToDisplayValue({}): start", value);

            buffer.setLength(0); // clear the old string

            try {
                if (showAsHex) {
                    if (isUINT64) {
                        buffer.append(Tools.toHexString((BigInteger) value, 8));
                    }
                    else {
                        buffer.append(Tools.toHexString(Long.valueOf(value.toString()), (int) typeSize));
                    }
                }
                else if (showAsBin) {
                    if (isUINT64) {
                        buffer.append(Tools.toBinaryString((BigInteger) value, 8));
                    }
                    else {
                        buffer.append(Tools.toBinaryString(Long.valueOf(value.toString()), (int) typeSize));
                    }
                }
                else if (numberFormat != null) {
                    buffer.append(numberFormat.format(value));
                }
                else {
                    buffer.append(value.toString());
                }
            }
            catch (Exception ex) {
                log.debug("NumericalDataDisplayConverter: canonicalToDisplayValue() failure: ", ex);
                buffer.append("*ERROR*");
            }

            log.trace("NumericalDataDisplayConverter: canonicalToDisplayValue({}): finish", buffer);

            return buffer;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            return value;
        }

    }

    private static class EnumDataDisplayConverter extends HDFDisplayConverter {

        private final StringBuffer buffer;

        private final H5Datatype   enumType;

        EnumDataDisplayConverter(final Datatype dtype) throws Exception {
            log.trace("EnumDataDisplayConverter: start");

            if (!dtype.isEnum()) {
                log.debug("EnumDataDisplayConverter: datatype is not an enum type");
                throw new Exception("EnumDataDisplayConverter: datatype is not an enum type");
            }

            buffer = new StringBuffer();

            enumType = (H5Datatype) dtype;

            log.trace("EnumDataDisplayConverter: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("EnumDataDisplayConverter: canonicalToDisplayValue({}): start", value);

            buffer.setLength(0); // clear the old string

            try {
                if (isEnumConverted) {
                    String[] retValues = null;

                    try {
                        retValues = enumType.convertEnumValueToName(value);
                    }
                    catch (HDF5Exception ex) {
                        log.trace("EnumDataDisplayConverter: canonicalToDisplayValue(): Could not convert enum values to names: ", ex);
                        retValues = null;
                    }

                    if (retValues != null)
                        buffer.append(retValues[0]);
                }
                else
                    buffer.append(value);
            }
            catch (Exception ex) {
                log.debug("EnumDataDisplayConverter: canonicalToDisplayValue() failure: ", ex);
                buffer.append("*ERROR*");
            }

            log.trace("EnumDataDisplayConverter: canonicalToDisplayValue({}): finish", buffer);

            return buffer;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            return value;
        }

    }

    private static class BitfieldDataDisplayConverter extends HDFDisplayConverter {

        private final StringBuffer buffer;

        private final boolean      isOpaque;

        BitfieldDataDisplayConverter(final Datatype dtype) throws Exception {
            log.trace("BitfieldDataDisplayConverter: start");

            if (!dtype.isBitField() && !dtype.isOpaque()) {
                log.debug("BitfieldDataDisplayConverter: datatype is not a bitfield or opaque type");
                throw new Exception("BitfieldDataDisplayConverter: datatype is not a bitfield or opaque type");
            }

            buffer = new StringBuffer();

            isOpaque = dtype.isOpaque();

            log.trace("BitfieldDataDisplayConverter: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("BitfieldDataDisplayConverter: canonicalToDisplayValue({}): start", value);

            buffer.setLength(0); // clear the old string

            try {
                for (int i = 0; i < ((byte[]) value).length; i++) {
                    if (i > 0) {
                        buffer.append(isOpaque ? " " : ":");
                    }

                    buffer.append(Tools.toHexString(Long.valueOf(((byte[]) value)[i]), 1));
                }
            }
            catch (Exception ex) {
                log.debug("BitfieldDataDisplayConverter: canonicalToDisplayValue() failure: ", ex);
                buffer.append("*ERROR*");
            }

            log.trace("BitfieldDataDisplayConverter: canonicalToDisplayValue({}): finish", buffer);

            return buffer;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            return value;
        }

    }

    private static class RefDataDisplayConverter extends HDFDisplayConverter {

        RefDataDisplayConverter(final Datatype dtype) throws Exception {
            log.trace("RefDataDisplayConverter: start");

            if (!dtype.isRef()) {
                log.debug("RefDataDisplayConverter: datatype is not a reference type");
                throw new Exception("RefDataDisplayConverter: datatype is not a reference type");
            }

            log.trace("RefDataDisplayConverter: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            return value;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            return value;
        }

    }

}
