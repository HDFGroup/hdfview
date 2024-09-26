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

package hdf.object;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The abstract class provides general APIs to create and manipulate dataset/attribute objects, and retrieve
 * dataset/attribute properties, datatype and dimension sizes.
 *
 * This class provides two convenient functions, read()/write(), to read/write data values. Reading/writing
 * data may take many library calls if we use the library APIs directly. The read() and write functions hide
 * all the details of these calls from users.
 *
 * For more details on dataset and attributes, See <a href=
 * "https://support.hdfgroup.org/releases/hdf5/v1_14/v1_14_5/documentation/doxygen/_h5_d__u_g.html#sec_dataset">HDF5
 * Datasets in HDF5 User Guide</a> <a href=
 * "https://support.hdfgroup.org/releases/hdf5/v1_14/v1_14_5/documentation/doxygen/_h5_a__u_g.html#sec_attribute">HDF5
 * Attributes in HDF5 User Guide</a>
 *
 * @see hdf.object.ScalarDS
 * @see hdf.object.CompoundDS
 *
 * @version 1.1 9/4/2007
 * @author Peter X. Cao
 */
public abstract class Dataset extends HObject implements DataFormat {
    private static final long serialVersionUID = -3360885430038261178L;

    private static final Logger log = LoggerFactory.getLogger(Dataset.class);

    /**
     * The memory buffer that holds the raw data array of the dataset.
     */
    protected transient Object data;

    /**
     * The type of space for the dataset.
     */
    protected int space_type;

    /**
     * The number of dimensions of the dataset.
     */
    protected int rank;

    /**
     * The current dimension sizes of the dataset
     */
    protected long[] dims;

    /**
     * The max dimension sizes of the dataset
     */
    protected long[] maxDims;

    /**
     * Array that contains the number of data points selected (for read/write)
     * in each dimension.
     *
     * The selected size must be less than or equal to the current dimension size.
     * A subset of a rectangle selection is defined by the starting position and
     * selected sizes.
     *
     * For example, if a 4 X 5 dataset is as follows:
     *
     * <pre>
     *     0,  1,  2,  3,  4
     *    10, 11, 12, 13, 14
     *    20, 21, 22, 23, 24
     *    30, 31, 32, 33, 34
     * long[] dims = {4, 5};
     * long[] startDims = {1, 2};
     * long[] selectedDims = {3, 3};
     * then the following subset is selected by the startDims and selectedDims above:
     *     12, 13, 14
     *     22, 23, 24
     *     32, 33, 34
     * </pre>
     */
    protected long[] selectedDims;

    /**
     * The starting position of each dimension of a selected subset. With both
     * the starting position and selected sizes, the subset of a rectangle
     * selection is fully defined.
     */
    protected long[] startDims;

    /**
     * Array that contains the indices of the dimensions selected for display.
     *
     * <B>selectedIndex[] is provided for two purposes:</B>
     * <OL>
     * <LI>
     * selectedIndex[] is used to indicate the order of dimensions for display,
     * i.e. selectedIndex[0] = row, selectedIndex[1] = column and
     * selectedIndex[2] = depth. For example, for a four dimension dataset, if
     * selectedIndex[] is {1, 2, 3}, then dim[1] is selected as row index,
     * dim[2] is selected as column index and dim[3] is selected as depth index.
     * <LI>
     * selectedIndex[] is also used to select dimensions for display for
     * datasets with three or more dimensions. We assume that applications such
     * as HDFView can only display data up to three dimensions (a 2D
     * spreadsheet/image with a third dimension that the 2D spreadsheet/image is
     * cut from). For datasets with more than three dimensions, we need
     * selectedIndex[] to store which three dimensions are chosen for display.
     * For example, for a four dimension dataset, if selectedIndex[] = {1, 2, 3},
     * then dim[1] is selected as row index, dim[2] is selected as column index
     * and dim[3] is selected as depth index. dim[0] is not selected. Its
     * location is fixed at 0 by default.
     * </OL>
     */
    protected final int[] selectedIndex;

    /**
     * The number of elements to move from the start location in each dimension.
     * For example, if selectedStride[0] = 2, every other data point is selected
     * along dim[0].
     */
    protected long[] selectedStride;

    /**
     * The array of dimension sizes for a chunk.
     */
    protected long[] chunkSize;

    /** The compression information. */
    protected StringBuilder compression;
    /** The compression information default prefix. */
    public static final String COMPRESSION_GZIP_TXT = "GZIP: level = ";

    /** The filters information. */
    protected StringBuilder filters;

    /** The storage layout information. */
    protected StringBuilder storageLayout;

    /** The storage information. */
    protected StringBuilder storage;

    /** The datatype object of the dataset. */
    protected Datatype datatype;

    /**
     * Array of strings that represent the dimension names. It is null if dimension names do not exist.
     */
    protected String[] dimNames;

    /** Flag to indicate if the byte[] array is converted to strings */
    protected boolean convertByteToString = true;

    /** Flag to indicate if data values are loaded into memory. */
    protected boolean isDataLoaded = false;

    /** Flag to indicate if this dataset has been initialized */
    protected boolean inited = false;

    /** The number of data points in the memory buffer. */
    protected long nPoints = 1;

    /** Flag to indicate if the dataspace is NULL */
    protected boolean isNULL = false;

    /** Flag to indicate if the data is a single scalar point */
    protected boolean isScalar = false;

    /** True if this dataset is an image. */
    protected boolean isImage = false;

    /** True if this dataset is ASCII text. */
    protected boolean isText = false;

    /**
     * The data buffer that contains the raw data directly reading from file
     * (before any data conversion).
     */
    protected transient Object originalBuf = null;

    /**
     * The array that holds the converted data of unsigned C-type integers.
     *
     * For example, Suppose that the original data is an array of unsigned
     * 16-bit short integers. Since Java does not support unsigned integer, the
     * data is converted to an array of 32-bit singed integer. In that case, the
     * converted buffer is the array of 32-bit singed integer.
     */
    protected transient Object convertedBuf = null;

    /**
     * Constructs a Dataset object with a given file, name and path.
     *
     * @param theFile
     *            the file that contains the dataset.
     * @param dsName
     *            the name of the Dataset, e.g. "dset1".
     * @param dsPath
     *            the full group path of this Dataset, e.g. "/arrays/".
     */
    public Dataset(FileFormat theFile, String dsName, String dsPath) { this(theFile, dsName, dsPath, null); }

    /**
     * @deprecated Not for public use in the future. <br>
     *             Using {@link #Dataset(FileFormat, String, String)}
     *
     * @param theFile
     *            the file that contains the dataset.
     * @param dsName
     *            the name of the Dataset, e.g. "dset1".
     * @param dsPath
     *            the full group path of this Dataset, e.g. "/arrays/".
     * @param oid
     *            the oid of this Dataset.
     */
    @Deprecated
    public Dataset(FileFormat theFile, String dsName, String dsPath, long[] oid)
    {
        super(theFile, dsName, dsPath, oid);
        log.trace("Dataset: start {}", dsName);

        datatype       = null;
        rank           = -1;
        space_type     = -1;
        data           = null;
        dims           = null;
        maxDims        = null;
        selectedDims   = null;
        startDims      = null;
        selectedStride = null;
        chunkSize      = null;
        compression    = new StringBuilder("NONE");
        filters        = new StringBuilder("NONE");
        storageLayout  = new StringBuilder("NONE");
        storage        = new StringBuilder("NONE");
        dimNames       = null;

        selectedIndex    = new int[3];
        selectedIndex[0] = 0;
        selectedIndex[1] = 1;
        selectedIndex[2] = 2;
    }

    /**
     * Clears memory held by the dataset, such as the data buffer.
     */
    @SuppressWarnings("rawtypes")
    public void clear()
    {
        if (data != null) {
            if (data instanceof List)
                ((List)data).clear();
            data         = null;
            originalBuf  = null;
            convertedBuf = null;
        }
        isDataLoaded = false;
    }

    /**
     * Returns the type of space for the dataset.
     *
     * @return the type of space for the dataset.
     */
    @Override
    public final int getSpaceType()
    {
        return space_type;
    }

    /**
     * Returns the rank (number of dimensions) of the dataset.
     *
     * @return the number of dimensions of the dataset.
     */
    @Override
    public final int getRank()
    {
        return rank;
    }

    /**
     * Returns the array that contains the dimension sizes of the dataset.
     *
     * @return the dimension sizes of the dataset.
     */
    @Override
    public final long[] getDims()
    {
        return dims;
    }

    /**
     * Returns the array that contains the max dimension sizes of the dataset.
     *
     * @return the max dimension sizes of the dataset.
     */
    public final long[] getMaxDims()
    {
        if (maxDims == null)
            return dims;

        return maxDims;
    }

    /**
     * Returns the dimension sizes of the selected subset.
     *
     * The SelectedDims is the number of data points of the selected subset.
     * Applications can use this array to change the size of selected subset.
     *
     * The selected size must be less than or equal to the current dimension size.
     * Combined with the starting position, selected sizes and stride, the
     * subset of a rectangle selection is fully defined.
     *
     * For example, if a 4 X 5 dataset is as follows:
     *
     * <pre>
     *     0,  1,  2,  3,  4
     *    10, 11, 12, 13, 14
     *    20, 21, 22, 23, 24
     *    30, 31, 32, 33, 34
     * long[] dims = {4, 5};
     * long[] startDims = {1, 2};
     * long[] selectedDims = {3, 3};
     * long[] selectedStride = {1, 1};
     * then the following subset is selected by the startDims and selectedDims
     *     12, 13, 14
     *     22, 23, 24
     *     32, 33, 34
     * </pre>
     *
     * @return the dimension sizes of the selected subset.
     */
    @Override
    public final long[] getSelectedDims()
    {
        return selectedDims;
    }

    /**
     * Returns the starting position of a selected subset.
     *
     * Applications can use this array to change the starting position of a
     * selection. Combined with the selected dimensions, selected sizes and
     * stride, the subset of a rectangle selection is fully defined.
     *
     * For example, if a 4 X 5 dataset is as follows:
     *
     * <pre>
     *     0,  1,  2,  3,  4
     *    10, 11, 12, 13, 14
     *    20, 21, 22, 23, 24
     *    30, 31, 32, 33, 34
     * long[] dims = {4, 5};
     * long[] startDims = {1, 2};
     * long[] selectedDims = {3, 3};
     * long[] selectedStride = {1, 1};
     * then the following subset is selected by the startDims and selectedDims
     *     12, 13, 14
     *     22, 23, 24
     *     32, 33, 34
     * </pre>
     *
     * @return the starting position of a selected subset.
     */
    @Override
    public final long[] getStartDims()
    {
        return startDims;
    }

    /**
     * Returns the selectedStride of the selected dataset.
     *
     * Applications can use this array to change how many elements to move in
     * each dimension.
     *
     * Combined with the starting position and selected sizes, the subset of a
     * rectangle selection is defined.
     *
     * For example, if a 4 X 5 dataset is as follows:
     *
     * <pre>
     *     0,  1,  2,  3,  4
     *    10, 11, 12, 13, 14
     *    20, 21, 22, 23, 24
     *    30, 31, 32, 33, 34
     * long[] dims = {4, 5};
     * long[] startDims = {0, 0};
     * long[] selectedDims = {2, 2};
     * long[] selectedStride = {2, 3};
     * then the following subset is selected by the startDims and selectedDims
     *     0,   3
     *     20, 23
     * </pre>
     *
     * @return the selectedStride of the selected dataset.
     */
    @Override
    public final long[] getStride()
    {
        if (rank <= 0)
            return null;

        if (selectedStride == null) {
            selectedStride = new long[rank];
            for (int i = 0; i < rank; i++)
                selectedStride[i] = 1;
        }

        return selectedStride;
    }

    /**
     * Sets the flag that indicates if a byte array is converted to a string
     * array.
     *
     * In a string dataset, the raw data from file is stored in a byte array. By
     * default, this byte array is converted to an array of strings. For a large
     * dataset (e.g. more than one million strings), the conversion takes a long
     * time and requires a lot of memory space to store the strings. In some
     * applications, such a conversion can be delayed. For example, A GUI
     * application may convert only the part of the strings that is visible to the
     * users, not the entire data array.
     *
     * setConvertByteToString(boolean b) allows users to set the flag so that
     * applications can choose to perform the byte-to-string conversion or not.
     * If the flag is set to false, the getData() returns an array of byte
     * instead of an array of strings.
     *
     * @param b
     *            convert bytes to strings if b is true; otherwise, if false, do
     *            not convert bytes to strings.
     */
    public final void setConvertByteToString(boolean b) { convertByteToString = b; }

    /**
     * Returns the flag that indicates if a byte array is converted to a string
     * array.
     *
     * @return true if byte array is converted to string; otherwise, returns
     *         false if there is no conversion.
     */
    public final boolean getConvertByteToString() { return convertByteToString; }

    /**
     * Reads the raw data of the dataset from file to a byte array.
     *
     * readBytes() reads raw data to an array of bytes instead of array of its
     * datatype. For example, for a one-dimension 32-bit integer dataset of
     * size 5, readBytes() returns a byte array of size 20 instead of an
     * int array of 5.
     *
     * readBytes() can be used to copy data from one dataset to another
     * efficiently because the raw data is not converted to its native type, it
     * saves memory space and CPU time.
     *
     * @return the byte array of the raw data.
     *
     * @throws Exception if data can not be read
     */
    public abstract byte[] readBytes() throws Exception;

    /**
     * Writes the memory buffer of this dataset to file.
     *
     * @throws Exception if buffer can not be written
     */
    @Override
    public final void write() throws Exception
    {
        log.trace("Dataset: write enter");
        if (data != null) {
            log.trace("Dataset: write data");
            write(data);
        }
    }

    /**
     * Creates a new dataset and writes the data buffer to the new dataset.
     *
     * This function allows applications to create a new dataset for a given
     * data buffer. For example, users can select a specific interesting part
     * from a large image and create a new image with the selection.
     *
     * The new dataset retains the datatype and dataset creation properties of
     * this dataset.
     *
     * @param pgroup
     *            the group which the dataset is copied to.
     * @param name
     *            the name of the new dataset.
     * @param dims
     *            the dimension sizes of the the new dataset.
     * @param data
     *            the data values of the subset to be copied.
     *
     * @return the new dataset.
     *
     * @throws Exception if dataset can not be copied
     */
    public abstract Dataset copy(Group pgroup, String name, long[] dims, Object data) throws Exception;

    /**
     * The status of initialization for this object
     *
     * @return true if the data has been initialized
     */
    @Override
    public final boolean isInited()
    {
        return inited;
    }

    /**
     * Resets selection of dataspace
     */
    protected void resetSelection()
    {
        for (int i = 0; i < rank; i++) {
            startDims[i]    = 0;
            selectedDims[i] = 1;
            if (selectedStride != null)
                selectedStride[i] = 1;
        }

        if (rank == 1) {
            selectedIndex[0] = 0;
            selectedDims[0]  = dims[0];
        }
        else if (rank == 2) {
            selectedIndex[0] = 0;
            selectedIndex[1] = 1;
            selectedDims[0]  = dims[0];
            selectedDims[1]  = dims[1];
        }
        else if (rank > 2) {
            if (isImage) {
                // 3D dataset is arranged in the order of [frame][height][width]
                selectedIndex[1] = rank - 1; // width, the fastest dimension
                selectedIndex[0] = rank - 2; // height
                selectedIndex[2] = rank - 3; // frames
            }
            else {
                selectedIndex[0] = 0; // width, the fastest dimension
                selectedIndex[1] = 1; // height
                selectedIndex[2] = 2; // frames
            }

            selectedDims[selectedIndex[0]] = dims[selectedIndex[0]];
            selectedDims[selectedIndex[1]] = dims[selectedIndex[1]];
            selectedDims[selectedIndex[2]] = dims[selectedIndex[2]];
        }

        isDataLoaded = false;
    }

    /**
     * Returns the data buffer of the dataset in memory.
     *
     * If data is already loaded into memory, returns the data; otherwise, calls
     * read() to read data from file into a memory buffer and returns the memory
     * buffer.
     *
     * By default, the whole dataset is read into memory. Users can also select
     * a subset to read. Subsetting is done in an implicit way.
     *
     * <b>How to Select a Subset</b>
     *
     * A selection is specified by three arrays: start, stride and count.
     * <ol>
     * <li>start: offset of a selection
     * <li>stride: determines how many elements to move in each dimension
     * <li>count: number of elements to select in each dimension
     * </ol>
     * getStartDims(), getStride() and getSelectedDims() returns the start,
     * stride and count arrays respectively. Applications can make a selection
     * by changing the values of the arrays.
     *
     * The following example shows how to make a subset. In the example, the
     * dataset is a 4-dimensional array of [200][100][50][10], i.e. dims[0]=200;
     * dims[1]=100; dims[2]=50; dims[3]=10; <br>
     * We want to select every other data point in dims[1] and dims[2]
     *
     * <pre>
     * int rank = dataset.getRank(); // number of dimensions of the dataset
     * long[] dims = dataset.getDims(); // the dimension sizes of the dataset
     * long[] selected = dataset.getSelectedDims(); // the selected size of the dataet
     * long[] start = dataset.getStartDims(); // the offset of the selection
     * long[] stride = dataset.getStride(); // the stride of the dataset
     * int[] selectedIndex = dataset.getSelectedIndex(); // the selected dimensions for display
     *
     * // select dim1 and dim2 as 2D data for display,and slice through dim0
     * selectedIndex[0] = 1;
     * selectedIndex[1] = 2;
     * selectedIndex[1] = 0;
     *
     * // reset the selection arrays
     * for (int i = 0; i &lt; rank; i++) {
     *     start[i] = 0;
     *     selected[i] = 1;
     *     stride[i] = 1;
     * }
     *
     * // set stride to 2 on dim1 and dim2 so that every other data point is
     * // selected.
     * stride[1] = 2;
     * stride[2] = 2;
     *
     * // set the selection size of dim1 and dim2
     * selected[1] = dims[1] / stride[1];
     * selected[2] = dims[1] / stride[2];
     *
     * // when dataset.getData() is called, the selection above will be used since
     * // the dimension arrays are passed by reference. Changes of these arrays
     * // outside the dataset object directly change the values of these array
     * // in the dataset object.
     * </pre>
     *
     * For ScalarDS, the memory data buffer is a one-dimensional array of byte,
     * short, int, float, double or String type based on the datatype of the
     * dataset.
     *
     * For CompoundDS, the memory data object is an java.util.List object. Each
     * element of the list is a data array that corresponds to a compound field.
     *
     * For example, if compound dataset "comp" has the following nested
     * structure, and member datatypes
     *
     * <pre>
     * comp --&gt; m01 (int)
     * comp --&gt; m02 (float)
     * comp --&gt; nest1 --&gt; m11 (char)
     * comp --&gt; nest1 --&gt; m12 (String)
     * comp --&gt; nest1 --&gt; nest2 --&gt; m21 (long)
     * comp --&gt; nest1 --&gt; nest2 --&gt; m22 (double)
     * </pre>
     *
     * getData() returns a list of six arrays: {int[], float[], char[],
     * String[], long[] and double[]}.
     *
     * @return the memory buffer of the dataset.
     *
     * @throws Exception if object can not be read
     * @throws OutOfMemoryError if memory is exhausted
     */
    @Override
    public Object getData() throws Exception, OutOfMemoryError
    {
        log.trace("getData(): isDataLoaded={}", isDataLoaded);
        if (!isDataLoaded) {
            data = read(); // load the data
            if (data != null) {
                originalBuf  = data;
                isDataLoaded = true;
                nPoints      = 1;
                log.trace("getData(): selectedDims length={}", selectedDims.length);
                for (int j = 0; j < selectedDims.length; j++)
                    nPoints *= selectedDims[j];
            }
            log.trace("getData(): read {}", nPoints);
        }

        return data;
    }

    /**
     * Not for public use in the future.
     *
     * setData() is not safe to use because it changes memory buffer
     * of the dataset object. Dataset operations such as write/read
     * will fail if the buffer type or size is changed.
     *
     * @param d  the object data -must be an array of Objects
     */
    @Override
    public final void setData(Object d)
    {
        if (!(this instanceof Attribute))
            throw new UnsupportedOperationException("setData: unsupported for non-Attribute objects");

        log.trace("setData(): isDataLoaded={}", isDataLoaded);
        data         = d;
        originalBuf  = data;
        isDataLoaded = true;
    }

    /**
     * Clears the current data buffer in memory and forces the next read() to load
     * the data from file.
     *
     * The function read() loads data from file into memory only if the data is
     * not read. If data is already in memory, read() just returns the memory
     * buffer. Sometimes we want to force read() to re-read data from file. For
     * example, when the selection is changed, we need to re-read the data.
     *
     * @see #getData()
     * @see #read()
     */
    @Override
    public void clearData()
    {
        isDataLoaded = false;
    }

    /**
     * Refreshes the current object in the file.
     *
     * The function read() loads data from file into memory only if the data is not
     * read. If data is already in memory, read() just returns the memory buffer.
     * Sometimes we want to force a clear and read to re-read the object from the file.
     * For example, when the selection is changed, we need to re-read the data.
     *
     * @see #getData()
     * @see #read()
     */
    @Override
    public Object refreshData()
    {
        Object dataValue = null;

        clearData();
        try {
            dataValue = getData();

            /*
             * TODO: Converting data from unsigned C integers to Java integers
             *       is currently unsupported for Compound Datasets.
             */
            if (!(this instanceof CompoundDS))
                convertFromUnsignedC();

            dataValue = getData();
            log.trace("refresh data");
        }
        catch (Exception ex) {
            log.trace("refresh data failure: ", ex);
        }
        return dataValue;
    }

    /**
     * Returns the dimension size of the vertical axis.
     *
     * This function is used by GUI applications such as HDFView. GUI
     * applications display a dataset in a 2D table or 2D image. The display
     * order is specified by the index array of selectedIndex as follow:
     * <dl>
     * <dt>selectedIndex[0] -- height</dt>
     * <dd>The vertical axis</dd>
     * <dt>selectedIndex[1] -- width</dt>
     * <dd>The horizontal axis</dd>
     * <dt>selectedIndex[2] -- depth</dt>
     * <dd>The depth axis is used for 3 or more dimensional datasets.</dd>
     * </dl>
     * Applications can use getSelectedIndex() to access and change the display
     * order. For example, in a 2D dataset of 200x50 (dim0=200, dim1=50), the
     * following code will set the height=200 and width=50.
     *
     * <pre>
     * int[] selectedIndex = dataset.getSelectedIndex();
     * selectedIndex[0] = 0;
     * selectedIndex[1] = 1;
     * </pre>
     *
     * @see #getSelectedIndex()
     * @see #getWidth()
     *
     * @return the size of dimension of the vertical axis.
     */
    @Override
    public final long getHeight()
    {
        if ((selectedDims == null) || (selectedIndex == null))
            return 0;

        if ((selectedDims.length < 1) || (selectedIndex.length < 1))
            return 0;

        log.trace("getHeight {}", selectedDims[selectedIndex[0]]);
        return selectedDims[selectedIndex[0]];
    }

    /**
     * Returns the dimension size of the horizontal axis.
     *
     * This function is used by GUI applications such as HDFView. GUI
     * applications display a dataset in 2D Table or 2D Image. The display order is
     * specified by the index array of selectedIndex as follow:
     * <dl>
     * <dt>selectedIndex[0] -- height</dt>
     * <dd>The vertical axis</dd>
     * <dt>selectedIndex[1] -- width</dt>
     * <dd>The horizontal axis</dd>
     * <dt>selectedIndex[2] -- depth</dt>
     * <dd>The depth axis, which is used for 3 or more dimension datasets.</dd>
     * </dl>
     * Applications can use getSelectedIndex() to access and change the display
     * order. For example, in a 2D dataset of 200x50 (dim0=200, dim1=50), the
     * following code will set the height=200 and width=100.
     *
     * <pre>
     * int[] selectedIndex = dataset.getSelectedIndex();
     * selectedIndex[0] = 0;
     * selectedIndex[1] = 1;
     * </pre>
     *
     * @see #getSelectedIndex()
     * @see #getHeight()
     *
     * @return the size of dimension of the horizontal axis.
     */
    @Override
    public final long getWidth()
    {
        if ((selectedDims == null) || (selectedIndex == null))
            return 0;

        if ((selectedDims.length < 2) || (selectedIndex.length < 2))
            return 1;

        log.trace("getWidth {}", selectedDims[selectedIndex[1]]);
        return selectedDims[selectedIndex[1]];
    }

    /**
     * Returns the dimension size of the frame axis.
     *
     * This function is used by GUI applications such as HDFView. GUI
     * applications display a dataset in 2D Table or 2D Image. The display order is
     * specified by the index array of selectedIndex as follow:
     * <dl>
     * <dt>selectedIndex[0] -- height</dt>
     * <dd>The vertical axis</dd>
     * <dt>selectedIndex[1] -- width</dt>
     * <dd>The horizontal axis</dd>
     * <dt>selectedIndex[2] -- depth</dt>
     * <dd>The depth axis, which is used for 3 or more dimension datasets.</dd>
     * </dl>
     * Applications can use getSelectedIndex() to access and change the display
     * order. For example, in a 2D dataset of 200x50 (dim0=200, dim1=50), the
     * following code will set the height=200 and width=100.
     *
     * <pre>
     * int[] selectedIndex = dataset.getSelectedIndex();
     * selectedIndex[0] = 0;
     * selectedIndex[1] = 1;
     * </pre>
     *
     * @see #getSelectedIndex()
     * @see #getHeight()
     *
     * @return the size of dimension of the frame axis.
     */
    @Override
    public final long getDepth()
    {
        if ((selectedDims == null) || (selectedIndex == null))
            return 0;

        if ((selectedDims.length < 2) || (selectedIndex.length < 2))
            return 1;

        log.trace("getDepth {}", selectedDims[selectedIndex[2]]);
        return selectedDims[selectedIndex[2]];
    }

    /**
     * Returns the indices of display order.
     *
     * selectedIndex[] is provided for two purposes:
     * <OL>
     * <LI>
     * selectedIndex[] is used to indicate the order of dimensions for display.
     * selectedIndex[0] is for the row, selectedIndex[1] is for the column and
     * selectedIndex[2] for the depth.
     *
     * For example, for a four dimension dataset, if selectedIndex[] = {1, 2, 3},
     * then dim[1] is selected as row index, dim[2] is selected as column index
     * and dim[3] is selected as depth index.
     * <LI>
     * selectedIndex[] is also used to select dimensions for display for
     * datasets with three or more dimensions. We assume that applications such
     * as HDFView can only display data values up to three dimensions (2D
     * spreadsheet/image with a third dimension which the 2D spreadsheet/image
     * is selected from). For datasets with more than three dimensions, we need
     * selectedIndex[] to tell applications which three dimensions are chosen
     * for display. <br>
     * For example, for a four dimension dataset, if selectedIndex[] = {1, 2, 3},
     * then dim[1] is selected as row index, dim[2] is selected as column index
     * and dim[3] is selected as depth index. dim[0] is not selected. Its
     * location is fixed at 0 by default.
     * </OL>
     *
     * @return the array of the indices of display order.
     */
    @Override
    public final int[] getSelectedIndex()
    {
        return selectedIndex;
    }

    /**
     * Returns the string representation of compression information.
     *
     * For example,
     * "SZIP: Pixels per block = 8: H5Z_FILTER_CONFIG_DECODE_ENABLED".
     *
     * @return the string representation of compression information.
     */
    @Override
    public final String getCompression()
    {
        return compression.toString();
    }

    /**
     * Returns the string representation of filter information.
     *
     * @return the string representation of filter information.
     */
    public final String getFilters() { return filters.toString(); }

    /**
     * Returns the string representation of storage layout information.
     *
     * @return the string representation of storage layout information.
     */
    public final String getStorageLayout() { return storageLayout.toString(); }

    /**
     * Returns the string representation of storage information.
     *
     * @return the string representation of storage information.
     */
    public final String getStorage() { return storage.toString(); }

    /**
     * Returns the array that contains the dimension sizes of the chunk of the
     * dataset. Returns null if the dataset is not chunked.
     *
     * @return the array of chunk sizes or returns null if the dataset is not
     *         chunked.
     */
    public final long[] getChunkSize() { return chunkSize; }

    /**
     * Returns the datatype of the data object.
     *
     * @return the datatype of the data object.
     */
    @Override
    public Datatype getDatatype()
    {
        return datatype;
    }

    /**
     * @deprecated Not for public use in the future. <br>
     *             Using {@link #convertFromUnsignedC(Object, Object)}
     *
     * @param dataIN  the object data
     *
     * @return the converted object
     */
    @Deprecated
    public static Object convertFromUnsignedC(Object dataIN)
    {
        return Dataset.convertFromUnsignedC(dataIN, null);
    }

    /**
     * Converts one-dimension array of unsigned C-type integers to a new array
     * of appropriate Java integer in memory.
     *
     * Since Java does not support unsigned integer, values of unsigned C-type
     * integers must be converted into its appropriate Java integer. Otherwise,
     * the data value will not displayed correctly. For example, if an unsigned
     * C byte, x = 200, is stored into an Java byte y, y will be -56 instead of
     * the correct value of 200.
     *
     * Unsigned C integers are upgrade to Java integers according to the
     * following table:
     *  <table border=1>
     * <caption><b>Mapping Unsigned C Integers to Java Integers</b></caption>
     * <TR>
     * <TD><B>Unsigned C Integer</B></TD>
     * <TD><B>JAVA Intege</B>r</TD>
     * </TR>
     * <TR>
     * <TD>unsigned byte</TD>
     * <TD>signed short</TD>
     * </TR>
     * <TR>
     * <TD>unsigned short</TD>
     * <TD>signed int</TD>
     * </TR>
     * <TR>
     * <TD>unsigned int</TD>
     * <TD>signed long</TD>
     * </TR>
     * <TR>
     * <TD>unsigned long</TD>
     * <TD>signed long</TD>
     * </TR>
     * </TABLE>
     * <strong>NOTE: this conversion cannot deal with unsigned 64-bit integers.
     * Therefore, the values of unsigned 64-bit datasets may be wrong in Java
     * applications</strong>.
     *
     * If memory data of unsigned integers is converted by
     * convertFromUnsignedC(), convertToUnsignedC() must be called to convert
     * the data back to unsigned C before data is written into file.
     *
     * @see #convertToUnsignedC(Object, Object)
     *
     * @param dataIN
     *            the input 1D array of the unsigned C-type integers.
     * @param dataOUT
     *            the output converted (or upgraded) 1D array of Java integers.
     *
     * @return the upgraded 1D array of Java integers.
     */
    @SuppressWarnings("rawtypes")
    public static Object convertFromUnsignedC(Object dataIN, Object dataOUT)
    {
        if (dataIN == null) {
            log.debug("convertFromUnsignedC(): data_in is null");
            return null;
        }

        Class dataClass = dataIN.getClass();
        if (!dataClass.isArray()) {
            log.debug("convertFromUnsignedC(): data_in not an array");
            return null;
        }

        if (dataOUT != null) {
            Class dataClassOut = dataOUT.getClass();
            if (!dataClassOut.isArray() || (Array.getLength(dataIN) != Array.getLength(dataOUT))) {
                log.debug("convertFromUnsignedC(): data_out not an array or does not match data_in size");
                dataOUT = null;
            }
        }

        String cname = dataClass.getName();
        char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
        int size     = Array.getLength(dataIN);
        log.trace("convertFromUnsignedC(): cname={} dname={} size={}", cname, dname, size);

        if (dname == 'B') {
            log.trace("convertFromUnsignedC(): Java convert byte to short");
            short[] sdata = null;
            if (dataOUT == null)
                sdata = new short[size];
            else
                sdata = (short[])dataOUT;

            byte[] bdata = (byte[])dataIN;
            for (int i = 0; i < size; i++)
                sdata[i] = (short)((bdata[i] + 256) & 0xFF);

            dataOUT = sdata;
        }
        else if (dname == 'S') {
            log.trace("convertFromUnsignedC(): Java convert short to int");
            int[] idata = null;
            if (dataOUT == null)
                idata = new int[size];
            else
                idata = (int[])dataOUT;

            short[] sdata = (short[])dataIN;
            for (int i = 0; i < size; i++)
                idata[i] = (sdata[i] + 65536) & 0xFFFF;

            dataOUT = idata;
        }
        else if (dname == 'I') {
            log.trace("convertFromUnsignedC(): Java convert int to long");
            long[] ldata = null;
            if (dataOUT == null)
                ldata = new long[size];
            else
                ldata = (long[])dataOUT;

            int[] idata = (int[])dataIN;
            for (int i = 0; i < size; i++)
                ldata[i] = (idata[i] + 4294967296L) & 0xFFFFFFFFL;

            dataOUT = ldata;
        }
        else {
            dataOUT = dataIN;
            log.debug("convertFromUnsignedC(): Java does not support unsigned long");
        }

        return dataOUT;
    }

    /**
     * @deprecated Not for public use in the future. <br>
     *             Using {@link #convertToUnsignedC(Object, Object)}
     *
     * @param dataIN
     *            the input 1D array of the unsigned C-type integers.
     *
     * @return the upgraded 1D array of Java integers.
     */
    @Deprecated
    public static Object convertToUnsignedC(Object dataIN)
    {
        return Dataset.convertToUnsignedC(dataIN, null);
    }

    /**
     * Converts the array of converted unsigned integers back to unsigned C-type
     * integer data in memory.
     *
     * If memory data of unsigned integers is converted by
     * convertFromUnsignedC(), convertToUnsignedC() must be called to convert
     * the data back to unsigned C before data is written into file.
     *
     * @see #convertFromUnsignedC(Object, Object)
     *
     * @param dataIN
     *            the input array of the Java integer.
     * @param dataOUT
     *            the output array of the unsigned C-type integer.
     *
     * @return the converted data of unsigned C-type integer array.
     */
    @SuppressWarnings("rawtypes")
    public static Object convertToUnsignedC(Object dataIN, Object dataOUT)
    {
        if (dataIN == null) {
            log.debug("convertToUnsignedC(): data_in is null");
            return null;
        }

        Class dataClass = dataIN.getClass();
        if (!dataClass.isArray()) {
            log.debug("convertToUnsignedC(): data_in not an array");
            return null;
        }

        if (dataOUT != null) {
            Class dataClassOut = dataOUT.getClass();
            if (!dataClassOut.isArray() || (Array.getLength(dataIN) != Array.getLength(dataOUT))) {
                log.debug("convertToUnsignedC(): data_out not an array or does not match data_in size");
                dataOUT = null;
            }
        }

        String cname = dataClass.getName();
        char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
        int size     = Array.getLength(dataIN);
        log.trace("convertToUnsignedC(): cname={} dname={} size={}", cname, dname, size);

        if (dname == 'S') {
            log.trace("convertToUnsignedC(): Java convert short to byte");
            byte[] bdata = null;
            if (dataOUT == null)
                bdata = new byte[size];
            else
                bdata = (byte[])dataOUT;
            short[] sdata = (short[])dataIN;
            for (int i = 0; i < size; i++)
                bdata[i] = (byte)sdata[i];
            dataOUT = bdata;
        }
        else if (dname == 'I') {
            log.trace("convertToUnsignedC(): Java convert int to short");
            short[] sdata = null;
            if (dataOUT == null)
                sdata = new short[size];
            else
                sdata = (short[])dataOUT;
            int[] idata = (int[])dataIN;
            for (int i = 0; i < size; i++)
                sdata[i] = (short)idata[i];
            dataOUT = sdata;
        }
        else if (dname == 'J') {
            log.trace("convertToUnsignedC(): Java convert long to int");
            int[] idata = null;
            if (dataOUT == null)
                idata = new int[size];
            else
                idata = (int[])dataOUT;
            long[] ldata = (long[])dataIN;
            for (int i = 0; i < size; i++)
                idata[i] = (int)ldata[i];
            dataOUT = idata;
        }
        else {
            dataOUT = dataIN;
            log.debug("convertToUnsignedC(): Java does not support unsigned long");
        }

        return dataOUT;
    }

    /**
     * Converts an array of bytes into an array of Strings for a fixed string
     * dataset.
     *
     * A C-string is an array of chars while an Java String is an object. When a
     * string dataset is read into a Java application, the data is stored in an
     * array of Java bytes. byteToString() is used to convert the array of bytes
     * into an array of Java strings so that applications can display and modify
     * the data content.
     *
     * For example, the content of a two element C string dataset is {"ABC",
     * "abc"}. Java applications will read the data into a byte array of {65,
     * 66, 67, 97, 98, 99). byteToString(bytes, 3) returns an array of Java
     * String of strs[0]="ABC", and strs[1]="abc".
     *
     * If memory data of strings is converted to Java Strings, stringToByte()
     * must be called to convert the memory data back to byte array before data
     * is written to file.
     *
     * @see #stringToByte(String[], int)
     *
     * @param bytes
     *            the array of bytes to convert.
     * @param length
     *            the length of string.
     *
     * @return the array of Java String.
     */
    public static final String[] byteToString(byte[] bytes, int length)
    {
        if (bytes == null) {
            log.debug("byteToString(): input is null");
            return null;
        }

        int n = bytes.length / length;
        log.trace("byteToString(): n={} from length of {}", n, length);
        String[] strArray = new String[n];
        String str        = null;
        int idx           = 0;
        for (int i = 0; i < n; i++) {
            str = new String(bytes, i * length, length);
            idx = str.indexOf('\0');
            if (idx >= 0)
                str = str.substring(0, idx);

            // trim only the end
            int end = str.length();
            while (end > 0 && str.charAt(end - 1) <= '\u0020')
                end--;

            strArray[i] = (end <= 0) ? "" : str.substring(0, end);
        }

        return strArray;
    }

    /**
     * Converts a string array into an array of bytes for a fixed string
     * dataset.
     *
     * If memory data of strings is converted to Java Strings, stringToByte()
     * must be called to convert the memory data back to byte array before data
     * is written to file.
     *
     * @see #byteToString(byte[] bytes, int length)
     *
     * @param strings
     *            the array of string.
     * @param length
     *            the length of string.
     *
     * @return the array of bytes.
     */
    public static final byte[] stringToByte(String[] strings, int length)
    {
        if (strings == null) {
            log.debug("stringToByte(): input is null");
            return null;
        }

        int size     = strings.length;
        byte[] bytes = new byte[size * length];
        log.trace("stringToByte(): size={} length={}", size, length);
        StringBuilder strBuff = new StringBuilder(length);
        for (int i = 0; i < size; i++) {
            // initialize the string with spaces
            strBuff.replace(0, length, " ");

            if (strings[i] != null) {
                if (strings[i].length() > length)
                    strings[i] = strings[i].substring(0, length);
                strBuff.replace(0, length, strings[i]);
            }

            strBuff.setLength(length);
            System.arraycopy(strBuff.toString().getBytes(), 0, bytes, length * i, length);
        }

        return bytes;
    }

    /**
     * Returns the array of strings that represent the dimension names. Returns
     * null if there is no dimension name.
     *
     * Some datasets have pre-defined names for each dimension such as
     * "Latitude" and "Longitude". getDimNames() returns these pre-defined
     * names.
     *
     * @return the names of dimensions, or null if there is no dimension name.
     */
    public final String[] getDimNames() { return dimNames; }

    /**
     * Checks if a given datatype is a string. Sub-classes must replace this
     * default implementation.
     *
     * @param tid
     *            The data type identifier.
     *
     * @return true if the datatype is a string; otherwise returns false.
     */
    public boolean isString(long tid) { return false; }

    /**
     * Returns the size in bytes of a given datatype. Sub-classes must replace
     * this default implementation.
     *
     * @param tid
     *            The data type identifier.
     *
     * @return The size of the datatype
     */
    public long getSize(long tid) { return -1; }

    /**
     * Get Class of the original data buffer if converted.
     *
     * @return the Class of originalBuf
     */
    @Override
    @SuppressWarnings("rawtypes")
    public final Class getOriginalClass()
    {
        return originalBuf.getClass();
    }

    /**
     * Check if dataset's dataspace is a NULL
     *
     * @return true if the dataspace is a NULL; otherwise, returns false.
     */
    public boolean isNULL() { return isNULL; }

    /**
     * Check if dataset is a single scalar point
     *
     * @return true if the data is a single scalar point; otherwise, returns false.
     */
    public boolean isScalar() { return isScalar; }

    /**
     * Checks if dataset is virtual. Sub-classes must replace
     * this default implementation.
     *
     * @return true if the dataset is virtual; otherwise returns false.
     */
    public boolean isVirtual() { return false; }

    /**
     * Gets the source file name at index if dataset is virtual. Sub-classes must replace
     * this default implementation.
     *
     * @param index
     *            index of the source file name if dataset is virtual.
     *
     * @return filename if the dataset is virtual; otherwise returns null.
     */
    public String getVirtualFilename(int index) { return null; }

    /**
     * Gets the number of source files if dataset is virtual. Sub-classes must replace
     * this default implementation.
     *
     * @return the list size if the dataset is virtual; otherwise returns negative.
     */
    public int getVirtualMaps() { return -1; }

    /**
     * Returns a string representation of the data value. For
     * example, "0, 255".
     *
     * For a compound datatype, it will be a 1D array of strings with field
     * members separated by the delimiter. For example,
     * "{0, 10.5}, {255, 20.0}, {512, 30.0}" is a compound attribute of {int,
     * float} of three data points.
     *
     * @param delimiter
     *            The delimiter used to separate individual data points. It
     *            can be a comma, semicolon, tab or space. For example,
     *            toString(",") will separate data by commas.
     *
     * @return the string representation of the data values.
     */
    public String toString(String delimiter) { return toString(delimiter, -1); }

    /**
     * Returns a string representation of the data value. For
     * example, "0, 255".
     *
     * For a compound datatype, it will be a 1D array of strings with field
     * members separated by the delimiter. For example,
     * "{0, 10.5}, {255, 20.0}, {512, 30.0}" is a compound attribute of {int,
     * float} of three data points.
     *
     * @param delimiter
     *            The delimiter used to separate individual data points. It
     *            can be a comma, semicolon, tab or space. For example,
     *            toString(",") will separate data by commas.
     * @param maxItems
     *            The maximum number of Array values to return
     *
     * @return the string representation of the data values.
     */
    public String toString(String delimiter, int maxItems)
    {
        Object theData = originalBuf;
        if (theData == null) {
            log.debug("toString: value is null");
            return null;
        }

        if (theData instanceof List<?>) {
            log.trace("toString: value is list");
            return null;
        }

        Class<? extends Object> valClass = theData.getClass();

        if (!valClass.isArray()) {
            log.trace("toString: finish - not array");
            String strValue = theData.toString();
            if (maxItems > 0 && strValue.length() > maxItems)
                // truncate the extra characters
                strValue = strValue.substring(0, maxItems);
            return strValue;
        }

        // value is an array
        int n = Array.getLength(theData);
        if ((maxItems > 0) && (n > maxItems))
            n = maxItems;

        return toString(theData, getDatatype(), delimiter, n);
    }

    /**
     * Returns a string representation of the dataset object.
     *
     * @param theData   The Object data
     * @param theType   The type of the data in the Object
     * @param delimiter The delimiter used to separate individual data points. It can be a comma, semicolon,
     *     tab or
     *                  space. For example, toString(",") will separate data by commas.
     * @param count     The maximum number of Array values to return
     *
     * @return the string representation of the dataset object.
     */
    protected String toString(Object theData, Datatype theType, String delimiter, int count)
    {
        log.trace("toString: is_enum={} is_unsigned={} Array.getLength={}", theType.isEnum(),
                  theType.isUnsigned(), count);
        StringBuilder sb                 = new StringBuilder();
        Class<? extends Object> valClass = theData.getClass();

        if (theType.isEnum()) {
            String cname = valClass.getName();
            char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
            log.trace("toString: is_enum with cname={} dname={}", cname, dname);

            Map<String, String> map = theType.getEnumMembers();
            String theValue         = null;
            switch (dname) {
            case 'B':
                byte[] barray = (byte[])theData;
                short sValue  = barray[0];
                theValue      = String.valueOf(sValue);
                if (map.containsKey(theValue))
                    sb.append(map.get(theValue));
                else
                    sb.append(sValue);
                for (int i = 1; i < count; i++) {
                    sb.append(delimiter);
                    sValue   = barray[i];
                    theValue = String.valueOf(sValue);
                    if (map.containsKey(theValue))
                        sb.append(map.get(theValue));
                    else
                        sb.append(sValue);
                }
                break;
            case 'S':
                short[] sarray = (short[])theData;
                int iValue     = sarray[0];
                theValue       = String.valueOf(iValue);
                if (map.containsKey(theValue))
                    sb.append(map.get(theValue));
                else
                    sb.append(iValue);
                for (int i = 1; i < count; i++) {
                    sb.append(delimiter);
                    iValue   = sarray[i];
                    theValue = String.valueOf(iValue);
                    if (map.containsKey(theValue))
                        sb.append(map.get(theValue));
                    else
                        sb.append(iValue);
                }
                break;
            case 'I':
                int[] iarray = (int[])theData;
                long lValue  = iarray[0];
                theValue     = String.valueOf(lValue);
                if (map.containsKey(theValue))
                    sb.append(map.get(theValue));
                else
                    sb.append(lValue);
                for (int i = 1; i < count; i++) {
                    sb.append(delimiter);
                    lValue   = iarray[i];
                    theValue = String.valueOf(lValue);
                    if (map.containsKey(theValue))
                        sb.append(map.get(theValue));
                    else
                        sb.append(lValue);
                }
                break;
            case 'J':
                long[] larray = (long[])theData;
                Long l        = larray[0];
                theValue      = Long.toString(l);
                if (map.containsKey(theValue))
                    sb.append(map.get(theValue));
                else
                    sb.append(theValue);
                for (int i = 1; i < count; i++) {
                    sb.append(delimiter);
                    l        = larray[i];
                    theValue = Long.toString(l);
                    if (map.containsKey(theValue))
                        sb.append(map.get(theValue));
                    else
                        sb.append(theValue);
                }
                break;
            default:
                sb.append(Array.get(theData, 0));
                for (int i = 1; i < count; i++) {
                    sb.append(delimiter);
                    sb.append(Array.get(theData, i));
                }
                break;
            }
        }
        else if (theType.isFloat() && theType.getDatatypeSize() == 2) {
            Object value = Array.get(theData, 0);
            String strValue;

            if (value == null)
                strValue = "null";
            else
                strValue = Float.toString(Float.float16ToFloat((short)value));

            // if (count > 0 && strValue.length() > count)
            // truncate the extra characters
            // strValue = strValue.substring(0, count);
            sb.append(strValue);

            for (int i = 1; i < count; i++) {
                sb.append(delimiter);
                value = Array.get(theData, i);

                if (value == null)
                    strValue = "null";
                else
                    strValue = Float.toString(Float.float16ToFloat((short)value));

                if (count > 0 && strValue.length() > count)
                    // truncate the extra characters
                    strValue = strValue.substring(0, count);
                sb.append(strValue);
            }
        }
        else if (theType.isUnsigned()) {
            String cname = valClass.getName();
            char dname   = cname.charAt(cname.lastIndexOf('[') + 1);
            log.trace("toString: is_unsigned with cname={} dname={}", cname, dname);

            switch (dname) {
            case 'B':
                byte[] barray = (byte[])theData;
                short sValue  = barray[0];
                if (sValue < 0)
                    sValue += 256;
                sb.append(sValue);
                for (int i = 1; i < count; i++) {
                    sb.append(delimiter);
                    sValue = barray[i];
                    if (sValue < 0)
                        sValue += 256;
                    sb.append(sValue);
                }
                break;
            case 'S':
                short[] sarray = (short[])theData;
                int iValue     = sarray[0];
                if (iValue < 0)
                    iValue += 65536;
                sb.append(iValue);
                for (int i = 1; i < count; i++) {
                    sb.append(delimiter);
                    iValue = sarray[i];
                    if (iValue < 0)
                        iValue += 65536;
                    sb.append(iValue);
                }
                break;
            case 'I':
                int[] iarray = (int[])theData;
                long lValue  = iarray[0];
                if (lValue < 0)
                    lValue += 4294967296L;
                sb.append(lValue);
                for (int i = 1; i < count; i++) {
                    sb.append(delimiter);
                    lValue = iarray[i];
                    if (lValue < 0)
                        lValue += 4294967296L;
                    sb.append(lValue);
                }
                break;
            case 'J':
                long[] larray   = (long[])theData;
                Long l          = larray[0];
                String theValue = Long.toString(l);
                if (l < 0) {
                    l               = (l << 1) >>> 1;
                    BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                    BigInteger big2 = new BigInteger(l.toString());
                    BigInteger big  = big1.add(big2);
                    theValue        = big.toString();
                }
                sb.append(theValue);
                for (int i = 1; i < count; i++) {
                    sb.append(delimiter);
                    l        = larray[i];
                    theValue = Long.toString(l);
                    if (l < 0) {
                        l               = (l << 1) >>> 1;
                        BigInteger big1 = new BigInteger("9223372036854775808"); // 2^65
                        BigInteger big2 = new BigInteger(l.toString());
                        BigInteger big  = big1.add(big2);
                        theValue        = big.toString();
                    }
                    sb.append(theValue);
                }
                break;
            default:
                String strValue = Array.get(theData, 0).toString();
                if (count > 0 && strValue.length() > count)
                    // truncate the extra characters
                    strValue = strValue.substring(0, count);
                sb.append(strValue);
                for (int i = 1; i < count; i++) {
                    sb.append(delimiter);
                    strValue = Array.get(theData, i).toString();
                    if (count > 0 && strValue.length() > count)
                        // truncate the extra characters
                        strValue = strValue.substring(0, count);
                    sb.append(strValue);
                }
                break;
            }
        }
        else if (theType.isVLEN() && !theType.isVarStr()) {
            log.trace("toString: vlen");
            String strValue;

            Object value = Array.get(theData, 0);
            if (value == null)
                strValue = "null";
            else {
                if (theType.getDatatypeBase().isRef()) {
                    if (theType.getDatatypeBase().getDatatypeSize() > 8)
                        strValue = "Region Reference";
                    else
                        strValue = "Object Reference";
                }
                else
                    strValue = value.toString();
            }
            sb.append(strValue);
        }
        else {
            log.trace("toString: not enum or unsigned");
            Object value = Array.get(theData, 0);
            String strValue;

            if (value == null)
                strValue = "null";
            else
                strValue = value.toString();

            // if (count > 0 && strValue.length() > count)
            //  truncate the extra characters
            //     strValue = strValue.substring(0, count);
            sb.append(strValue);

            for (int i = 1; i < count; i++) {
                sb.append(delimiter);
                value = Array.get(theData, i);

                if (value == null)
                    strValue = "null";
                else
                    strValue = value.toString();

                if (count > 0 && strValue.length() > count)
                    // truncate the extra characters
                    strValue = strValue.substring(0, count);
                sb.append(strValue);
            }
        }

        return sb.toString();
    }
}
