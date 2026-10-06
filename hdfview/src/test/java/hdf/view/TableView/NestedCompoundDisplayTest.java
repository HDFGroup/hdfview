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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import hdf.object.FileFormat;
import hdf.object.h5.H5CompoundDS;
import hdf.object.h5.H5File;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import hdf.view.TableView.DataDisplayConverterFactory.HDFDisplayConverter;
import hdf.view.TableView.DataProviderFactory.HDFDataProvider;

import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Cell text for a compound whose variable-length member sits inside a nested compound,
 * COMPOUND{id:int, inner:COMPOUND{v:VLEN of int}}, run through the same data provider and
 * display converter the table uses.
 */
public class NestedCompoundDisplayTest {

    @TempDir
    static Path tempDir;

    private static final String DATASET_NAME = "compound_of_nested_vlen";

    private static H5File testFile;
    private static HDFDataProvider provider;
    private static HDFDisplayConverter converter;

    private static ArrayList<Object> list(Object... items) { return new ArrayList<>(Arrays.asList(items)); }

    @BeforeAll
    static void createFile() throws Exception
    {
        String path = tempDir.resolve("nested_compound_display_test.h5").toString();

        long vt = -1, inner = -1, outer = -1, fid = -1, sid = -1, did = -1;
        try {
            vt    = H5.H5Tvlen_create(HDF5Constants.H5T_NATIVE_INT);
            inner = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, H5.H5Tget_size(vt));
            H5.H5Tinsert(inner, "v", 0, vt);

            long innerOffset = H5.H5Tget_size(HDF5Constants.H5T_NATIVE_INT);
            outer            = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, innerOffset + H5.H5Tget_size(inner));
            H5.H5Tinsert(outer, "id", 0, HDF5Constants.H5T_NATIVE_INT);
            H5.H5Tinsert(outer, "inner", innerOffset, inner);

            fid = H5.H5Fcreate(path, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT,
                               HDF5Constants.H5P_DEFAULT);
            sid = H5.H5Screate_simple(1, new long[] {2}, null);
            did = H5.H5Dcreate(fid, DATASET_NAME, outer, sid, HDF5Constants.H5P_DEFAULT,
                               HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);

            Object[] buf = {list(1, list(list(10, 11))), list(2, list(list(20)))};
            H5.H5DwriteVL(did, outer, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT,
                          buf);
        }
        finally {
            if (did >= 0)
                H5.H5Dclose(did);
            if (sid >= 0)
                H5.H5Sclose(sid);
            if (fid >= 0)
                H5.H5Fclose(fid);
            if (outer >= 0)
                H5.H5Tclose(outer);
            if (inner >= 0)
                H5.H5Tclose(inner);
            if (vt >= 0)
                H5.H5Tclose(vt);
        }

        testFile = (H5File)(new H5File()).createInstance(path, FileFormat.READ);
        testFile.open();

        H5CompoundDS dataset = (H5CompoundDS)testFile.get("/" + DATASET_NAME);
        dataset.init();
        Object data = dataset.getData();

        provider  = DataProviderFactory.getDataProvider(dataset, data, false);
        converter = DataDisplayConverterFactory.getDataDisplayConverter(dataset);
    }

    @AfterAll
    static void closeFile() throws Exception
    {
        if (testFile != null)
            testFile.close();
    }

    /** A table cell reduced to the row and column indices the converter reads. */
    private static ILayerCell cell(int row, int column)
    {
        return (ILayerCell)Proxy.newProxyInstance(
            ILayerCell.class.getClassLoader(), new Class<?>[] {ILayerCell.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                case "getRowIndex":
                    return row;
                case "getColumnIndex":
                    return column;
                default:
                    throw new UnsupportedOperationException(method.getName());
                }
            });
    }

    /** The text the table shows for one cell. */
    private static String cellText(int row, int column)
    {
        Object value = provider.getDataValue(column, row);
        return String.valueOf(converter.canonicalToDisplayValue(cell(row, column), null, value));
    }

    @Test
    void fixedMemberBesideNestedCompound()
    {
        assertEquals("1", cellText(0, 0));
        assertEquals("2", cellText(1, 0));
    }

    @Test
    void vlenMemberInsideNestedCompound()
    {
        assertEquals("[10, 11]", cellText(0, 1));
        assertEquals("[20]", cellText(1, 1));
    }
}
