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

/**
 * An interface that provides general I/O operations for object data. For
 * example, reading data content from the file into memory or writing data
 * content from memory into the file.
 * <p>
 *
 * @see hdf.object.HObject
 *
 * @version 1.0 4/2/2018
 * @author Jordan T. Henderson
 */
public interface DataFormat {
    public abstract boolean isInited();

    public abstract void init();

    /**
     * Retrieves the object's data from the file.
     *
     * @return the object's data.
     *
     * @throws Exception
     *             if the data can not be retrieved
     */
    public abstract Object getData() throws Exception, OutOfMemoryError;

    /**
     *
     *
     * @param data
     *            the data to write.
     */
    public abstract void setData(Object data);

    /**
     * Clears the current data buffer in memory and forces the next read() to load
     * the data from file.
     * <p>
     * The function read() loads data from file into memory only if the data is not
     * read. If data is already in memory, read() just returns the memory buffer.
     * Sometimes we want to force read() to re-read data from file. For example,
     * when the selection is changed, we need to re-read the data.
     *
     * @see #getData()
     * @see #read()
     */
    public abstract void clearData();

    /**
     * Reads the data from file.
     * <p>
     * read() reads the data from file to a memory buffer and returns the memory
     * buffer. The dataset object does not hold the memory buffer. To store the
     * memory buffer in the dataset object, one must call getData().
     * <p>
     * By default, the whole dataset is read into memory. Users can also select
     * a subset to read. Subsetting is done in an implicit way.
     *
     * @return the data read from file.
     *
     * @see #getData()
     *
     * @throws Exception
     *             if object can not be read
     * @throws OutOfMemoryError
     *             if memory is exhausted
     */
    public abstract Object read() throws Exception, OutOfMemoryError;

    /**
     * Writes a memory buffer to the object in the file.
     *
     * @param buf
     *            the data to write
     *
     * @throws Exception
     *             if data can not be written
     */
    public abstract void write(Object buf) throws Exception;

    /**
     * Writes the current memory buffer to the object in the file.
     *
     * @throws Exception
     *             if data can not be written
     */
    public abstract void write() throws Exception;

    /**
     * Converts the data values of this data object to appropriate Java integers if
     * they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertFromUnsignedC(Object, Object)
     *
     * @return the converted data buffer.
     */
    public Object convertFromUnsignedC();

    /**
     * Converts Java integer data values of this data object back to unsigned C-type
     * integer data if they are unsigned integers.
     *
     * @see hdf.object.Dataset#convertToUnsignedC(Object)
     * @see hdf.object.Dataset#convertToUnsignedC(Object, Object)
     *
     * @return the converted data buffer.
     */
    public Object convertToUnsignedC();

    /**
     * Returns the fill values for the data object.
     *
     * @return the fill values for the data object.
     */
    public abstract Object getFillValue();

    /**
     * Returns the datatype of the data object.
     *
     * @return the datatype of the data object.
     */
    public abstract Datatype getDatatype();

    /**
     * Returns the rank (number of dimensions) of the data object. It returns a
     * negative number if it failed to retrieve the dimension information from
     * the file.
     *
     * @return the number of dimensions of the data object.
     */
    public abstract int getRank();

    /**
     * Returns the array that contains the dimension sizes of the data value of
     * the data object. It returns null if it failed to retrieve the dimension
     * information from the file.
     *
     * @return the dimension sizes of the data object.
     */
    public abstract long[] getDims();


    /****************************************************************
     * * The following four definitions are used for data subsetting. * *
     ****************************************************************/

    /**
     * Returns the dimension sizes of the selected subset.
     * <p>
     * The SelectedDims is the number of data points of the selected subset.
     * Applications can use this array to change the size of selected subset.
     *
     * The selected size must be less than or equal to the current dimension size.
     * Combined with the starting position, selected sizes and stride, the subset of
     * a rectangle selection is fully defined.
     * <p>
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
    public abstract long[] getSelectedDims();

    /**
     * Returns the starting position of a selected subset.
     * <p>
     * Applications can use this array to change the starting position of a
     * selection. Combined with the selected dimensions, selected sizes and stride,
     * the subset of a rectangle selection is fully defined.
     * <p>
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
    public abstract long[] getStartDims();

    /**
     * Returns the selectedStride of the selected dataset.
     * <p>
     * Applications can use this array to change how many elements to move in each
     * dimension.
     *
     * Combined with the starting position and selected sizes, the subset of a
     * rectangle selection is defined.
     * <p>
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
    public abstract long[] getStride();

    /**
     * Returns the indices of display order.
     * <p>
     *
     * selectedIndex[] is provided for two purposes:
     * <OL>
     * <LI>selectedIndex[] is used to indicate the order of dimensions for display.
     * selectedIndex[0] is for the row, selectedIndex[1] is for the column and
     * selectedIndex[2] for the depth.
     * <p>
     * For example, for a four dimension dataset, if selectedIndex[] = {1, 2, 3},
     * then dim[1] is selected as row index, dim[2] is selected as column index and
     * dim[3] is selected as depth index.
     * <LI>selectedIndex[] is also used to select dimensions for display for
     * datasets with three or more dimensions. We assume that applications such as
     * HDFView can only display data values up to three dimensions (2D
     * spreadsheet/image with a third dimension which the 2D spreadsheet/image is
     * selected from). For datasets with more than three dimensions, we need
     * selectedIndex[] to tell applications which three dimensions are chosen for
     * display. <br>
     * For example, for a four dimension dataset, if selectedIndex[] = {1, 2, 3},
     * then dim[1] is selected as row index, dim[2] is selected as column index and
     * dim[3] is selected as depth index. dim[0] is not selected. Its location is
     * fixed at 0 by default.
     * </OL>
     *
     * @return the array of the indices of display order.
     */
    public int[] getSelectedIndex();

    /**************************************************************************
     * * The following two definitions are used primarily for GUI applications. * *
     **************************************************************************/

    /**
     * Returns the dimension size of the vertical axis.
     *
     * <p>
     * This function is used by GUI applications such as HDFView. GUI applications
     * display a dataset in a 2D table or 2D image. The display order is specified
     * by the index array of selectedIndex as follow:
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
    public long getHeight();

    /**
     * Returns the dimension size of the horizontal axis.
     *
     * <p>
     * This function is used by GUI applications such as HDFView. GUI applications
     * display a dataset in 2D Table or 2D Image. The display order is specified by
     * the index array of selectedIndex as follow:
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
    public long getWidth();

    /**
     * Returns the string representation of compression information.
     * <p>
     * For example, "SZIP: Pixels per block = 8: H5Z_FILTER_CONFIG_DECODE_ENABLED".
     *
     * @return the string representation of compression information.
     */
    public abstract String getCompression();

    /**
     * Get runtime Class of the original data buffer if converted.
     *
     * @return the Class of the original data buffer
     */
    @SuppressWarnings("rawtypes")
    public abstract Class getOriginalClass();
}
