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

package hdf.view.TableView;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class DataDisplayConverterFactory
{
    private static final Logger log = LoggerFactory.getLogger(DataDisplayConverterFactory.class);

    /**
     * To keep things clean from an API perspective, keep a static reference to the last
     * CompoundDataFormat that was passed in. This keeps us from needing to pass the
     * CompoundDataFormat object as a parameter to every DataDisplayConverter class,
     * since it's really only needed by the CompoundDataDisplayConverter.
     */
    private static DataFormat dataFormatReference = null;

    /**
     * Get the Data Display Converter for the supplied data object
     *
     * @param dataObject
     *        the data object
     *
     * @return the converter instance
     *
     * @throws Exception if a failure occurred
     */
    public static HDFDisplayConverter getDataDisplayConverter(final DataFormat dataObject) throws Exception {
        if (dataObject == null) {
            log.debug("getDataDisplayConverter(DataFormat): data object is null");
            return null;
        }

        dataFormatReference = dataObject;

        HDFDisplayConverter converter = getDataDisplayConverter(dataObject.getDatatype());

        return converter;
    }

    private static final HDFDisplayConverter getDataDisplayConverter(final Datatype dtype) throws Exception {
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

        return converter;
    }

    /** the HDF extension for data converters */
    public static class HDFDisplayConverter extends DisplayConverter
    {
        private static final Logger log = LoggerFactory.getLogger(HDFDisplayConverter.class);

        /** the number format type */
        protected NumberFormat     numberFormat = null;
        /** if the data shows in hex format */
        protected boolean          showAsHex = false;
        /** if data shows in binary format */
        protected boolean          showAsBin = false;
        /** if the enum mapped value is shown */
        protected boolean          isEnumConverted = false;

        /**
         * This field is only used for CompoundDataDisplayConverters, but when the
         * top-level DisplayConverter is a "container" type, such as an
         * ArrayDataDisplayConverter, we have to set this field and pass it through in
         * case there is a CompoundDataDisplayConverter at the bottom of the chain.
         */
        /** the "container" type row index */
        protected int              cellRowIdx;
        /** the "container" type column index */
        protected int              cellColIdx;

        /** create a HDF data converter
         *
         * @param dtype
         *        the datatype for conversion
         */
        HDFDisplayConverter(final Datatype dtype) {
            cellRowIdx = -1;
            cellColIdx = -1;
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value instanceof String)
                return value;

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                return DataFactoryUtils.nullStr;
            }

            return value;
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            log.trace("displayToCanonicalValue({}): start", value);
            return value;
        }

        /**
         * set the number format type
         *
         * @param format
         *        the data format
         */
        public void setNumberFormat(NumberFormat format) {
            numberFormat = format;
        }

        /**
         * set if the data shows in hex format
         *
         * @param asHex
         *        if the data shows as hex format
         */
        public void setShowAsHex(boolean asHex) {
            showAsHex = asHex;
        }

        /**
         * set if data shows in binary format
         *
         * @param asBin
         *        if the data shows as binary format
         */
        public void setShowAsBin(boolean asBin) {
            showAsBin = asBin;
        }

        /**
         * set if the enum mapped value is shown
         *
         * @param convert
         *        if the enum data should be converted
         */
        public void setConvertEnum(boolean convert) {
            isEnumConverted = convert;
        }
    }

    private static class CompoundDataDisplayConverter extends HDFDisplayConverter
    {
        private static final Logger log = LoggerFactory.getLogger(CompoundDataDisplayConverter.class);

        private final HashMap<Integer, Integer> baseConverterIndexMap;
        private final HashMap<Integer, Integer> relCmpdStartIndexMap;
        private final HDFDisplayConverter[]     memberTypeConverters;
        private final StringBuilder             buffer;
        private final int                       nTotFields;

        CompoundDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            if (!dtype.isCompound()) {
                log.debug("datatype is not a compound type");
                throw new Exception("CompoundDataDisplayConverter: datatype is not a compound type");
            }

            CompoundDataFormat compoundFormat = (CompoundDataFormat) dataFormatReference;

            List<Datatype> localSelectedTypes = DataFactoryUtils.filterNonSelectedMembers(compoundFormat, dtype);

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
            HashMap<Integer, Integer>[] maps = DataFactoryUtils.buildIndexMaps(compoundFormat, localSelectedTypes);
            baseConverterIndexMap = maps[DataFactoryUtils.COL_TO_BASE_CLASS_MAP_INDEX];
            relCmpdStartIndexMap = maps[DataFactoryUtils.CMPD_START_IDX_MAP_INDEX];

            log.trace("index maps built: baseConverterIndexMap = {}, relColIdxMap = {}",
                    baseConverterIndexMap, relCmpdStartIndexMap);

            if (baseConverterIndexMap.size() == 0) {
                log.debug("base DataDisplayConverter index mapping is invalid - size 0");
                throw new Exception("CompoundDataDisplayConverter: invalid DataDisplayConverter mapping of size 0 built");
            }

            if (relCmpdStartIndexMap.size() == 0) {
                log.debug("compound field start index mapping is invalid - size 0");
                throw new Exception("CompoundDataDisplayConverter: invalid compound field start index mapping of size 0 built");
            }

            nTotFields = baseConverterIndexMap.size();

            buffer = new StringBuilder();
        }

        @Override
        public Object canonicalToDisplayValue(ILayerCell cell, IConfigRegistry configRegistry, Object value) {
            cellRowIdx = cell.getRowIndex();
            cellColIdx = cell.getColumnIndex() % nTotFields;
            return canonicalToDisplayValue(value);
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value instanceof String)
                return value;

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                return DataFactoryUtils.nullStr;
            }

            buffer.setLength(0); // clear the old string

            try {
                if (cellColIdx >= nTotFields)
                    cellColIdx %= nTotFields;

                if (value instanceof List) {
                    /*
                     * For Arrays of Compounds, we convert an entire list of data.
                     */
                    List<?> cmpdList = (List<?>) value;

                    buffer.append("{");
                    for (int i = 0; i < memberTypeConverters.length; i++) {
                        if (i > 0)
                            buffer.append(", ");

                        Object curObject = cmpdList.get(i);
                        if (curObject instanceof List)
                            buffer.append(memberTypeConverters[i].canonicalToDisplayValue(curObject));
                        else {
                            Object dataArrayValue = Array.get(curObject, cellRowIdx);
                            buffer.append(memberTypeConverters[i].canonicalToDisplayValue(dataArrayValue));
                        }
                    }
                    buffer.append("}");
                }
                else {
                    HDFDisplayConverter converter = memberTypeConverters[baseConverterIndexMap.get(cellColIdx)];
                    converter.cellRowIdx = cellRowIdx;
                    converter.cellColIdx = cellColIdx - relCmpdStartIndexMap.get(cellColIdx);

                    buffer.append(converter.canonicalToDisplayValue(value));
                }
            }
            catch (Exception ex) {
                log.debug("canonicalToDisplayValue({}): failure: ", value, ex);
                buffer.setLength(0);
                buffer.append(DataFactoryUtils.errStr);
            }

            return buffer;
        }

        @Override
        public void setNumberFormat(NumberFormat format) {
            super.setNumberFormat(format);

            for (int i = 0; i < memberTypeConverters.length; i++)
                memberTypeConverters[i].setNumberFormat(format);
        }

        @Override
        public void setShowAsHex(boolean asHex) {
            super.setShowAsHex(asHex);

            for (int i = 0; i < memberTypeConverters.length; i++)
                memberTypeConverters[i].setShowAsHex(asHex);
        }

        @Override
        public void setShowAsBin(boolean asBin) {
            super.setShowAsBin(asBin);

            for (int i = 0; i < memberTypeConverters.length; i++)
                memberTypeConverters[i].setShowAsBin(asBin);
        }

        @Override
        public void setConvertEnum(boolean convert) {
            super.setConvertEnum(convert);

            for (int i = 0; i < memberTypeConverters.length; i++)
                memberTypeConverters[i].setConvertEnum(convert);
        }

    }

    private static class ArrayDataDisplayConverter extends HDFDisplayConverter
    {
        private static final Logger log = LoggerFactory.getLogger(ArrayDataDisplayConverter.class);

        private final HDFDisplayConverter baseTypeConverter;
        private final StringBuilder       buffer;

        ArrayDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            if (!dtype.isArray()) {
                log.debug("exit: datatype is not an array type");
                throw new Exception("ArrayDataDisplayConverter: datatype is not an array type");
            }

            Datatype baseType = dtype.getDatatypeBase();

            if (baseType == null) {
                log.debug("exit: base datatype is null");
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
                log.debug("exit: couldn't get DataDisplayConverter for base datatype: ", ex);
                throw new Exception("ArrayDataDisplayConverter: couldn't get DataDisplayConverter for base datatype: " + ex.getMessage());
            }

            buffer = new StringBuilder();
        }

        @Override
        public Object canonicalToDisplayValue(ILayerCell cell, IConfigRegistry configRegistry, Object value) {
            cellRowIdx = cell.getRowIndex();
            cellColIdx = cell.getColumnIndex();
            return canonicalToDisplayValue(value);
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value instanceof String)
                return value;

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                return DataFactoryUtils.nullStr;
            }

            buffer.setLength(0); // clear the old string

            /*
             * Pass the cell's row and column index down in case there is a
             * CompoundDataDisplayConverter at the bottom of the chain.
             */
            baseTypeConverter.cellRowIdx = cellRowIdx;
            baseTypeConverter.cellColIdx = cellColIdx;

            try {
                Object obj;
                Object convertedValue;
                int arrLen = Array.getLength(value);

                log.trace("canonicalToDisplayValue({}): array length={}", value, arrLen);

                if (!(baseTypeConverter instanceof CompoundDataDisplayConverter))
                    buffer.append("[");

                for (int i = 0; i < arrLen; i++) {
                    if (i > 0)
                        buffer.append(", ");

                    obj = Array.get(value, i);

                    convertedValue = baseTypeConverter.canonicalToDisplayValue(obj);

                    buffer.append(convertedValue);
                }

                if (!(baseTypeConverter instanceof CompoundDataDisplayConverter))
                    buffer.append("]");
            }
            catch (Exception ex) {
                log.debug("canonicalToDisplayValue({}): failure: ", value, ex);
                buffer.setLength(0);
                buffer.append(DataFactoryUtils.errStr);
            }

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

    private static class VlenDataDisplayConverter extends HDFDisplayConverter
    {
        private static final Logger log = LoggerFactory.getLogger(VlenDataDisplayConverter.class);

        private final HDFDisplayConverter baseTypeConverter;
        private final StringBuilder       buffer;

        VlenDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            if (!dtype.isVLEN() || dtype.isVarStr()) {
                log.debug("exit: datatype is not a variable-length type or is a variable-length string type (use StringDataDisplayConverter)");
                throw new Exception("VlenDataDisplayConverter: datatype is not a variable-length type or is a variable-length string type (use StringDataDisplayConverter)");
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

            buffer = new StringBuilder();
        }

        @Override
        public Object canonicalToDisplayValue(ILayerCell cell, IConfigRegistry configRegistry, Object value) {
            cellRowIdx = cell.getRowIndex();
            cellColIdx = cell.getColumnIndex();
            return canonicalToDisplayValue(value);
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value instanceof String)
                return value;

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                return DataFactoryUtils.nullStr;
            }

            buffer.setLength(0); // clear the old string

            /*
             * Pass the cell's row and column index down in case there is a
             * CompoundDataDisplayConverter at the bottom of the chain.
             */
            baseTypeConverter.cellRowIdx = cellRowIdx;
            baseTypeConverter.cellColIdx = cellColIdx;

            try {
                Object obj;
                Object convertedValue;
                int arrLen = Array.getLength(value);

                log.trace("canonicalToDisplayValue({}): array length={}", value, arrLen);

                if (!(baseTypeConverter instanceof RefDataDisplayConverter))
                    buffer.append("[");

                for (int i = 0; i < arrLen; i++) {
                    if (i > 0)
                        buffer.append(", ");

                    obj = Array.get(value, i);

                    convertedValue = baseTypeConverter.canonicalToDisplayValue(obj);

                    buffer.append(convertedValue);
                }

                if (!(baseTypeConverter instanceof RefDataDisplayConverter))
                    buffer.append("]");
            }
            catch (Exception ex) {
                log.debug("canonicalToDisplayValue({}): failure: ", value, ex);
                buffer.setLength(0);
                buffer.append(DataFactoryUtils.errStr);
            }

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

    private static class StringDataDisplayConverter extends HDFDisplayConverter
    {
        private static final Logger log = LoggerFactory.getLogger(StringDataDisplayConverter.class);

        StringDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            if (!dtype.isString() && !dtype.isVarStr()) {
                log.debug("datatype is not a string type");
                throw new Exception("StringDataDisplayConverter: datatype is not a string type");
            }
        }
    }

    private static class CharDataDisplayConverter extends HDFDisplayConverter
    {
        private static final Logger log = LoggerFactory.getLogger(CharDataDisplayConverter.class);

        CharDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            if (!dtype.isChar()) {
                log.debug("datatype is not a character type");
                throw new Exception("CharDataDisplayConverter: datatype is not a character type");
            }
        }

        @Override
        public Object displayToCanonicalValue(Object value) {
            char charValue = ((String) value).charAt(0);
            return (int) charValue;
        }
    }

    private static class NumericalDataDisplayConverter extends HDFDisplayConverter
    {
        private static final Logger log = LoggerFactory.getLogger(NumericalDataDisplayConverter.class);

        private final StringBuilder buffer;
        private final long          typeSize;
        private final boolean       isUINT64;

        NumericalDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            if (!dtype.isInteger() && !dtype.isFloat()) {
                log.debug("datatype is not an integer or floating-point type");
                throw new Exception("NumericalDataDisplayConverter: datatype is not an integer or floating-point type");
            }

            buffer = new StringBuilder();

            typeSize = dtype.getDatatypeSize();
            isUINT64 = dtype.isUnsigned() && (typeSize == 8);
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value instanceof String)
                return value;

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                return DataFactoryUtils.nullStr;
            }

            buffer.setLength(0); // clear the old string

            try {
                if (showAsHex) {
                    if (isUINT64)
                        buffer.append(Tools.toHexString((BigInteger) value, 8));
                    else
                        buffer.append(Tools.toHexString(Long.valueOf(value.toString()), (int) typeSize));
                }
                else if (showAsBin) {
                    if (isUINT64)
                        buffer.append(Tools.toBinaryString((BigInteger) value, 8));
                    else
                        buffer.append(Tools.toBinaryString(Long.valueOf(value.toString()), (int) typeSize));
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
                buffer.setLength(0);
                buffer.append(DataFactoryUtils.errStr);
            }

            return buffer;
        }
    }

    private static class EnumDataDisplayConverter extends HDFDisplayConverter
    {
        private static final Logger log = LoggerFactory.getLogger(EnumDataDisplayConverter.class);

        private final StringBuilder buffer;
        private final H5Datatype    enumType;

        EnumDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            if (!dtype.isEnum()) {
                log.debug("datatype is not an enum type");
                throw new Exception("EnumDataDisplayConverter: datatype is not an enum type");
            }

            buffer = new StringBuilder();

            enumType = (H5Datatype) dtype;
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value instanceof String)
                return value;

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
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
                buffer.setLength(0);
                buffer.append(DataFactoryUtils.errStr);
            }

            return buffer;
        }
    }

    private static class BitfieldDataDisplayConverter extends HDFDisplayConverter
    {
        private static final Logger log = LoggerFactory.getLogger(BitfieldDataDisplayConverter.class);

        private final StringBuilder buffer;
        private final boolean       isOpaque;

        BitfieldDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            if (!dtype.isBitField() && !dtype.isOpaque()) {
                log.debug("datatype is not a bitfield or opaque type");
                throw new Exception("BitfieldDataDisplayConverter: datatype is not a bitfield or opaque type");
            }

            buffer = new StringBuilder();

            isOpaque = dtype.isOpaque();
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value instanceof String)
                return value;

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                return DataFactoryUtils.nullStr;
            }

            buffer.setLength(0); // clear the old string

            try {
                for (int i = 0; i < ((byte[]) value).length; i++) {
                    if (i > 0)
                        buffer.append(isOpaque ? " " : ":");

                    buffer.append(Tools.toHexString((((byte[]) value)[i]), 1));
                }
            }
            catch (Exception ex) {
                log.debug("canonicalToDisplayValue({}): failure: ", value, ex);
                buffer.setLength(0);
                buffer.append(DataFactoryUtils.errStr);
            }

            return buffer;
        }
    }

    private static class RefDataDisplayConverter extends HDFDisplayConverter
    {
        private static final Logger log = LoggerFactory.getLogger(RefDataDisplayConverter.class);

        private final StringBuilder buffer;

        RefDataDisplayConverter(final Datatype dtype) throws Exception {
            super(dtype);

            if (!dtype.isRef()) {
                log.debug("datatype is not a reference type");
                throw new Exception("RefDataDisplayConverter: datatype is not a reference type");
            }

            buffer = new StringBuilder();
        }

        @Override
        public Object canonicalToDisplayValue(ILayerCell cell, IConfigRegistry configRegistry, Object value) {
            cellRowIdx = cell.getRowIndex();
            cellColIdx = cell.getColumnIndex();
            log.trace("canonicalToDisplayValue({}) cellRowIdx={} cellColIdx={}: start", value, cellRowIdx, cellColIdx);
            return canonicalToDisplayValue(value);
        }

        @Override
        public Object canonicalToDisplayValue(Object value) {
            log.trace("canonicalToDisplayValue({}): start", value);

            if (value instanceof String)
                return value;

            if (value == null) {
                log.debug("canonicalToDisplayValue({}): value is null", value);
                return DataFactoryUtils.nullStr;
            }

            buffer.setLength(0); // clear the old string

            try {
                Object obj;
                Object convertedValue;
                int arrLen = Array.getLength(value);

                log.trace("canonicalToDisplayValue({}): array length={}", value, arrLen);

                for (int i = 0; i < arrLen; i++) {
                    if (i > 0)
                        buffer.append(", ");

                    obj = Array.get(value, i);

                    buffer.append(obj);
                }
            }
            catch (Exception ex) {
                log.debug("canonicalToDisplayValue({}): failure: ", value, ex);
                buffer.setLength(0);
                buffer.append(DataFactoryUtils.errStr);
            }

            return buffer;
        }
    }

}
