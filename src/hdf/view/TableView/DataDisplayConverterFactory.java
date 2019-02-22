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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
    private static DataFormat dataFormatReference = null;

    public static HDFDisplayConverter getDataDisplayConverter(final DataFormat dataObject) throws Exception {
        log.trace("getDataDisplayConverter(DataFormat): start");

        if (dataObject == null) {
            log.debug("getDataDisplayConverter(DataFormat): data object is null");
            return null;
        }

        dataFormatReference = dataObject;

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
            else if (dtype.isString() || dtype.isVarStr())
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

            converter = new HDFDisplayConverter(dtype);
        }

        log.trace("getDataDisplayConverter(Datatype): finish");

        return converter;
    }

    public static class HDFDisplayConverter extends DisplayConverter {

        protected org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HDFDisplayConverter.class);

        protected NumberFormat     numberFormat = null;

        protected boolean          showAsHex = false;
        protected boolean          showAsBin = false;
        protected boolean          isEnumConverted = false;

        /*
         * This field is only used for CompoundDataDisplayConverters, but when the
         * top-level DisplayConverter is a "container" type, such as an
         * ArrayDataDisplayConverter, we have to set this field and pass it through in
         * case there is a CompoundDataDisplayConverter at the bottom of the chain.
         */
        protected int              cellColIdx;

        HDFDisplayConverter(final Datatype dtype) {
            log.trace("constructor: start");

            cellColIdx = -1;

            log.trace("constructor: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                log.trace("canonicalToDisplayValue({}): finish", value);
                return DataFactoryUtils.nullStr;
            }

            log.trace("canonicalToDisplayValue({}): finish", value);

            return value;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            log.trace("displayToCanonicalValue({}): start", value);
            log.trace("displayToCanonicalValue({}): finish", value);

            return value;
        }

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

        private final HashMap<Integer, Integer> baseConverterIndexMap;
        private final HashMap<Integer, Integer> relCmpdStartIndexMap;

        private final HDFDisplayConverter[]     memberTypeConverters;

        private final StringBuffer              buffer;

        private final int                       nTotFields;

        CompoundDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            log = org.slf4j.LoggerFactory.getLogger(CompoundDataDisplayConverter.class);

            log.trace("constructor: start");

            if (!dtype.isCompound()) {
                log.debug("datatype is not a compound type");
                throw new Exception("CompoundDataDisplayConverter: datatype is not a compound type");
            }

            CompoundDataFormat compoundFormat = (CompoundDataFormat) dataFormatReference;

            List<Datatype> allSelectedMemberTypes = Arrays.asList(compoundFormat.getSelectedMemberTypes());
            if (allSelectedMemberTypes == null) {
                log.debug("selected compound member datatype list is null");
                throw new Exception("CompoundDataDisplayConverter: selected compound member datatype list is null");
            }

            List<Datatype> localSelectedTypes = DataFactoryUtils.filterNonSelectedMembers(allSelectedMemberTypes, dtype);

            log.trace("setting up {} base HDFDisplayConverters", localSelectedTypes.size());

            memberTypeConverters = new HDFDisplayConverter[localSelectedTypes.size()];
            for (int i = 0; i < memberTypeConverters.length; i++) {
                log.trace("retrieving DataDisplayConverter for member {}", i);

                try {
                    memberTypeConverters[i] = getDataDisplayConverter(localSelectedTypes.get(i));

                    /*
                     * Make base datatype converters inherit the data conversion settings.
                     */
                    memberTypeConverters[i].setShowAsHex(this.showAsHex);
                    memberTypeConverters[i].setShowAsBin(this.showAsBin);
                    memberTypeConverters[i].setNumberFormat(this.numberFormat);
                    memberTypeConverters[i].setConvertEnum(this.isEnumConverted);
                }
                catch (Exception ex) {
                    log.debug("failed to retrieve DataDisplayConverter for member {}: ", i, ex);
                    memberTypeConverters[i] = null;
                }
            }

            /*
             * Build necessary index maps.
             */
            HashMap<Integer, Integer>[] maps = DataFactoryUtils.buildIndexMaps(allSelectedMemberTypes, localSelectedTypes);
            baseConverterIndexMap = maps[DataFactoryUtils.COL_TO_BASE_CLASS_MAP_INDEX];
            relCmpdStartIndexMap = maps[DataFactoryUtils.CMPD_START_IDX_MAP_INDEX];

            log.trace("index maps built: baseConverterIndexMap = {}, relColIdxMap = {}",
                    baseConverterIndexMap.toString(), relCmpdStartIndexMap.toString());

            nTotFields = baseConverterIndexMap.size();

            buffer = new StringBuffer();

            log.trace("constructor: finish");
        }

        @Override
        public Object canonicalToDisplayValue(ILayerCell cell, IConfigRegistry configRegistry, Object value) {
            cellColIdx = cell.getColumnIndex() % nTotFields;
            return canonicalToDisplayValue(value);
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                log.trace("canonicalToDisplayValue({}): finish", value);
                return DataFactoryUtils.nullStr;
            }

            buffer.setLength(0); // clear the old string

            try {
                HDFDisplayConverter converter = memberTypeConverters[baseConverterIndexMap.get(cellColIdx)];
                converter.cellColIdx = cellColIdx - relCmpdStartIndexMap.get(cellColIdx);

                buffer.append(converter.canonicalToDisplayValue(value));
            }
            catch (Exception ex) {
                log.debug("canonicalToDisplayValue({}): failure: ", value, ex);
                buffer.append(DataFactoryUtils.errStr);
            }

            log.trace("canonicalToDisplayValue({}): finish", buffer);

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
            super(dtype);

            log = org.slf4j.LoggerFactory.getLogger(ArrayDataDisplayConverter.class);

            log.trace("constructor: start");

            if (!dtype.isArray()) {
                log.debug("datatype is not an array type");
                throw new Exception("ArrayDataDisplayConverter: datatype is not an array type");
            }

            Datatype baseType = dtype.getDatatypeBase();

            if (baseType == null) {
                log.debug("base datatype is null");
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
                log.debug("couldn't get DataDisplayConverter for base datatype: ", ex);
                throw new Exception("ArrayDataDisplayConverter: couldn't get DataDisplayConverter for base datatype: " + ex.getMessage());
            }

            buffer = new StringBuffer();

            log.trace("constructor: finish");
        }

        @Override
        public Object canonicalToDisplayValue(ILayerCell cell, IConfigRegistry configRegistry, Object value) {
            /*
             * TODO:
             */
            /* cellColIdx = cell.getColumnIndex() % nTotFields; */
            return canonicalToDisplayValue(value);
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                log.trace("canonicalToDisplayValue({}): finish", value);
                return DataFactoryUtils.nullStr;
            }

            buffer.setLength(0); // clear the old string

            try {
                Object obj;
                Object convertedValue;
                int arrLen = Array.getLength(value);

                log.trace("canonicalToDisplayValue({}): array length={}", value, arrLen);

                buffer.append("[");
                for (int i = 0; i < arrLen; i++) {
                    if (i > 0) buffer.append(", ");

                    obj = Array.get(value, i);

                    convertedValue = baseTypeConverter.canonicalToDisplayValue(obj);

                    buffer.append(convertedValue);
                }
                buffer.append("]");
            }
            catch (Exception ex) {
                log.debug("canonicalToDisplayValue({}): failure: ", value, ex);
                buffer.append(DataFactoryUtils.errStr);
            }

            log.trace("canonicalToDisplayValue({}): finish", buffer);

            return buffer;
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
            super(dtype);

            log = org.slf4j.LoggerFactory.getLogger(VlenDataDisplayConverter.class);

            log.trace("constructor: start");

            if (!dtype.isVLEN()) {
                log.debug("datatype is not a variable-length type");
                throw new Exception("VlenDataDisplayConverter: datatype is not a variable-length type");
            }

            Datatype baseType = dtype.getDatatypeBase();

            if (baseType == null) {
                log.debug("base datatype is null");
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
                log.debug("couldn't get DataDisplayConverter for base datatype: ", ex);
                throw new Exception("VlenDataDisplayConverter: couldn't get DataDisplayConverter for base datatype: " + ex.getMessage());
            }

            log.trace("constructor: finish");
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
            super(dtype);

            log = org.slf4j.LoggerFactory.getLogger(StringDataDisplayConverter.class);

            log.trace("constructor: start");

            if (!dtype.isString() && !dtype.isVarStr()) {
                log.debug("datatype is not a string type");
                throw new Exception("StringDataDisplayConverter: datatype is not a string type");
            }

            log.trace("constructor: finish");
        }

    }

    private static class CharDataDisplayConverter extends HDFDisplayConverter {

        CharDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            log = org.slf4j.LoggerFactory.getLogger(CharDataDisplayConverter.class);

            log.trace("constructor: start");

            if (!dtype.isChar()) {
                log.debug("datatype is not a character type");
                throw new Exception("CharDataDisplayConverter: datatype is not a character type");
            }

            log.trace("constructor: finish");
        }

    }

    private static class NumericalDataDisplayConverter extends HDFDisplayConverter {

        private final StringBuffer buffer;

        private final long         typeSize;

        private final boolean      isUINT64;

        NumericalDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            log = org.slf4j.LoggerFactory.getLogger(NumericalDataDisplayConverter.class);

            log.trace("constructor: start");

            if (!dtype.isInteger() && !dtype.isFloat()) {
                log.debug("datatype is not an integer or floating-point type");
                throw new Exception("NumericalDataDisplayConverter: datatype is not an integer or floating-point type");
            }

            buffer = new StringBuffer();

            typeSize = dtype.getDatatypeSize();
            isUINT64 = dtype.isUnsigned() && (typeSize == 8);

            log.trace("constructor: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                log.trace("canonicalToDisplayValue({}): finish", value);
                return DataFactoryUtils.nullStr;
            }

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
                log.debug("canonicalToDisplayValue({}): failure: ", value, ex);
                buffer.append(DataFactoryUtils.errStr);
            }

            log.trace("canonicalToDisplayValue({}): finish", buffer);

            return buffer;
        }

    }

    private static class EnumDataDisplayConverter extends HDFDisplayConverter {

        private final StringBuffer buffer;

        private final H5Datatype   enumType;

        EnumDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            log = org.slf4j.LoggerFactory.getLogger(EnumDataDisplayConverter.class);

            log.trace("constructor: start");

            if (!dtype.isEnum()) {
                log.debug("datatype is not an enum type");
                throw new Exception("EnumDataDisplayConverter: datatype is not an enum type");
            }

            buffer = new StringBuffer();

            enumType = (H5Datatype) dtype;

            log.trace("constructor: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                log.trace("canonicalToDisplayValue({}): finish", value);
                return DataFactoryUtils.nullStr;
            }

            buffer.setLength(0); // clear the old string

            try {
                if (isEnumConverted) {
                    String[] retValues = null;

                    try {
                        retValues = enumType.convertEnumValueToName(value);
                    }
                    catch (HDF5Exception ex) {
                        log.trace("canonicalToDisplayValue({}): Could not convert enum values to names: ", value, ex);
                        retValues = null;
                    }

                    if (retValues != null)
                        buffer.append(retValues[0]);
                    else
                        buffer.append(DataFactoryUtils.nullStr);
                }
                else
                    buffer.append(value);
            }
            catch (Exception ex) {
                log.debug("canonicalToDisplayValue({}): failure: ", value, ex);
                buffer.append(DataFactoryUtils.errStr);
            }

            log.trace("canonicalToDisplayValue({}): finish", buffer);

            return buffer;
        }

    }

    private static class BitfieldDataDisplayConverter extends HDFDisplayConverter {

        private final StringBuffer buffer;

        private final boolean      isOpaque;

        BitfieldDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            log = org.slf4j.LoggerFactory.getLogger(BitfieldDataDisplayConverter.class);

            log.trace("constructor: start");

            if (!dtype.isBitField() && !dtype.isOpaque()) {
                log.debug("datatype is not a bitfield or opaque type");
                throw new Exception("BitfieldDataDisplayConverter: datatype is not a bitfield or opaque type");
            }

            buffer = new StringBuffer();

            isOpaque = dtype.isOpaque();

            log.trace("constructor: finish");
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                log.trace("canonicalToDisplayValue({}): finish", value);
                return DataFactoryUtils.nullStr;
            }

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
                log.debug("canonicalToDisplayValue({}): failure: ", value, ex);
                buffer.append(DataFactoryUtils.errStr);
            }

            log.trace("canonicalToDisplayValue({}): finish", buffer);

            return buffer;
        }

    }

    private static class RefDataDisplayConverter extends HDFDisplayConverter {

        RefDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            log = org.slf4j.LoggerFactory.getLogger(RefDataDisplayConverter.class);

            log.trace("constructor: start");

            if (!dtype.isRef()) {
                log.debug("datatype is not a reference type");
                throw new Exception("RefDataDisplayConverter: datatype is not a reference type");
            }

            log.trace("constructor: finish");
        }

    }

}
