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

package uitest;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

/**
 * A compound whose member is an array of compound, COMPOUND{id:int, pairs:ARRAY[2] of
 * COMPOUND{x:int, y:int}}, opened in the table view.
 */
public class TestCompoundArrayMemberDisplay extends AbstractWindowTest {

    private static final String FILENAME     = "compound_array_member.h5";
    private static final String DATASET_NAME = "/records";

    /**
     * Written into the work directory rather than committed, since the shape matters and
     * the contents do not.
     */
    @BeforeAll
    public static void createTestFile() throws Exception
    {
        File target = new File(workDir, FILENAME);
        if (target.exists() && !target.delete())
            throw new Exception("could not replace " + target);

        long inner = -1, arr = -1, outer = -1, fid = -1, sid = -1, did = -1;
        try {
            inner = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8);
            H5.H5Tinsert(inner, "x", 0, HDF5Constants.H5T_NATIVE_INT);
            H5.H5Tinsert(inner, "y", 4, HDF5Constants.H5T_NATIVE_INT);

            arr = H5.H5Tarray_create(inner, 1, new long[] {2});

            long idSize = H5.H5Tget_size(HDF5Constants.H5T_NATIVE_INT);
            outer       = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, idSize + H5.H5Tget_size(arr));
            H5.H5Tinsert(outer, "id", 0, HDF5Constants.H5T_NATIVE_INT);
            H5.H5Tinsert(outer, "pairs", idSize, arr);

            fid = H5.H5Fcreate(target.getAbsolutePath(), HDF5Constants.H5F_ACC_TRUNC,
                               HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            sid = H5.H5Screate_simple(1, new long[] {2}, null);
            did = H5.H5Dcreate(fid, DATASET_NAME, outer, sid, HDF5Constants.H5P_DEFAULT,
                               HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);

            java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(2 * 5 * 4)
                                          .order(java.nio.ByteOrder.nativeOrder());
            buf.putInt(1).putInt(10).putInt(11).putInt(12).putInt(13);
            buf.putInt(2).putInt(20).putInt(21).putInt(22).putInt(23);

            H5.H5Dwrite(did, outer, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                        HDF5Constants.H5P_DEFAULT, buf.array());
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
            if (arr >= 0)
                H5.H5Tclose(arr);
            if (inner >= 0)
                H5.H5Tclose(inner);
        }
    }

    @Test
    public void testCompoundArrayMemberDisplay()
    {
        String[][] expectedData = {{"1", "10", "11", "12", "13"}, {"2", "20", "21", "22", "23"}};

        SWTBotShell tableShell = null;
        File hdfFile           = openFile(FILENAME, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testCompoundArrayMemberDisplay()", 2, FILENAME);

            tableShell                     = openTreeviewObject(filetree, FILENAME, DATASET_NAME);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(
                dataTable, "testCompoundArrayMemberDisplay()", false);
            retriever.setContainerHeaderOffset(2, 0);

            retriever.testAllTableLocations(expectedData);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
        finally {
            closeShell(tableShell);

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
