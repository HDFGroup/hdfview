/**
 * Utilities and providers for JUnit 5 parameterized tests
 * Provides common parameter sources for HDF testing scenarios
 */
package test;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import object.H5TestFile;

/**
 * Support class for parameterized testing with JUnit 5
 * Provides common argument sources for HDF5 testing scenarios
 */
public class ParameterizedTestSupport {

    /**
     * Provide arguments for testing different HDF5 data types
     * @return Stream of Arguments containing (dataTypeName, expectedClass, testValue)
     */
    public static Stream<Arguments> dataTypeArguments() {
        return Stream.of(
            Arguments.of("Integer", Integer.class, 42),
            Arguments.of("Float", Float.class, 3.14f),
            Arguments.of("Double", Double.class, 2.718281828),
            Arguments.of("String", String.class, "TestString"),
            Arguments.of("Boolean", Boolean.class, true)
        );
    }

    /**
     * Provide arguments for testing standard test datasets
     * @return Stream of Arguments containing (datasetName, expectedType)
     */
    public static Stream<Arguments> standardDatasetArguments() {
        return Stream.of(
            Arguments.of(H5TestFile.NAME_DATASET_INT, "integer"),
            Arguments.of(H5TestFile.NAME_DATASET_FLOAT, "float"),
            Arguments.of(H5TestFile.NAME_DATASET_CHAR, "character"),
            Arguments.of(H5TestFile.NAME_DATASET_STR, "string"),
            Arguments.of(H5TestFile.NAME_DATASET_ENUM, "enum"),
            Arguments.of(H5TestFile.NAME_DATASET_COMPOUND, "compound"),
            Arguments.of(H5TestFile.NAME_DATASET_IMAGE, "image")
        );
    }

    /**
     * Provide arguments for testing different array sizes
     * @return Stream of Arguments containing (size, description)
     */
    public static Stream<Arguments> arraySizeArguments() {
        return Stream.of(
            Arguments.of(1, "single element"),
            Arguments.of(10, "small array"),
            Arguments.of(100, "medium array"),
            Arguments.of(1000, "large array")
        );
    }

    /**
     * Provide arguments for testing different file format scenarios
     * @return Stream of Arguments containing (formatName, extension, description)
     */
    public static Stream<Arguments> fileFormatArguments() {
        return Stream.of(
            Arguments.of("HDF5", ".h5", "Standard HDF5 format"),
            Arguments.of("HDF5", ".hdf5", "Extended HDF5 format"),
            Arguments.of("NetCDF", ".nc", "NetCDF format")
            // Note: HDF4 support depends on native libraries being available
        );
    }

    /**
     * Provide arguments for testing different error conditions
     * @return Stream of Arguments containing (errorCondition, expectedExceptionType)
     */
    public static Stream<Arguments> errorConditionArguments() {
        return Stream.of(
            Arguments.of("null_file", NullPointerException.class),
            Arguments.of("invalid_path", IllegalArgumentException.class),
            Arguments.of("missing_dataset", RuntimeException.class)
        );
    }

    /**
     * Provide arguments for testing various dataset dimensions
     * @return Stream of Arguments containing (dimensions, description)
     */
    public static Stream<Arguments> dimensionArguments() {
        return Stream.of(
            Arguments.of(new long[]{10}, "1D array"),
            Arguments.of(new long[]{5, 5}, "2D square matrix"),
            Arguments.of(new long[]{2, 3, 4}, "3D tensor"),
            Arguments.of(new long[]{100, 1}, "column vector"),
            Arguments.of(new long[]{1, 100}, "row vector")
        );
    }

    /**
     * Create test arguments for boundary value testing
     * @return Stream of Arguments for boundary conditions
     */
    public static Stream<Arguments> boundaryValueArguments() {
        return Stream.of(
            Arguments.of(0, "zero"),
            Arguments.of(1, "minimum positive"),
            Arguments.of(-1, "maximum negative"),
            Arguments.of(Integer.MAX_VALUE, "maximum integer"),
            Arguments.of(Integer.MIN_VALUE, "minimum integer")
        );
    }
}