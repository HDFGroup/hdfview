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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

/**
 * Export Data to Text File from the table view must write readable cell text with the default
 * display settings, for flat data and for variable-length members holding nested compounds.
 */
public class TestTableViewExport extends AbstractWindowTest {

    private static final String FILENAME = "table_export.h5";

    private static ArrayList<Object> list(Object... items) { return new ArrayList<>(Arrays.asList(items)); }

    @BeforeAll
    public static void createTestFile() throws Exception
    {
        File target = new File(workDir, FILENAME);
        if (target.exists() && !target.delete())
            throw new Exception("could not replace " + target);

        long intSize = H5.H5Tget_size(HDF5Constants.H5T_NATIVE_INT);
        long fid = -1, ivlen = -1, nested = -1, pt = -1, item = -1, cvlen = -1, multi = -1;
        try {
            fid = H5.H5Fcreate(target.getAbsolutePath(), HDF5Constants.H5F_ACC_TRUNC,
                               HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);

            // regular: 2x3 int
            write(fid, "regular", HDF5Constants.H5T_NATIVE_INT, new long[] {2, 3},
                  new int[] {1, 2, 3, 4, 5, 6});

            // nested: COMPOUND{id:int, v:VLEN of int}
            ivlen  = H5.H5Tvlen_create(HDF5Constants.H5T_NATIVE_INT);
            nested = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, intSize + H5.H5Tget_size(ivlen));
            H5.H5Tinsert(nested, "id", 0, HDF5Constants.H5T_NATIVE_INT);
            H5.H5Tinsert(nested, "v", intSize, ivlen);
            write(fid, "nested", nested, new long[] {2},
                  new Object[] {list(1, list(10, 11)), list(2, list(20))});

            // multi_nested: COMPOUND{id:int, items:VLEN of COMPOUND{k:int, pt:COMPOUND{x:int, y:int}}}
            pt = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 2 * intSize);
            H5.H5Tinsert(pt, "x", 0, HDF5Constants.H5T_NATIVE_INT);
            H5.H5Tinsert(pt, "y", intSize, HDF5Constants.H5T_NATIVE_INT);
            item = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, intSize + H5.H5Tget_size(pt));
            H5.H5Tinsert(item, "k", 0, HDF5Constants.H5T_NATIVE_INT);
            H5.H5Tinsert(item, "pt", intSize, pt);
            cvlen = H5.H5Tvlen_create(item);
            multi = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, intSize + H5.H5Tget_size(cvlen));
            H5.H5Tinsert(multi, "id", 0, HDF5Constants.H5T_NATIVE_INT);
            H5.H5Tinsert(multi, "items", intSize, cvlen);
            write(fid, "multi_nested", multi, new long[] {2},
                  new Object[] {list(1, list(list(1, list(2, 5)), list(3, list(4, 6)))),
                                list(2, list(list(5, list(6, 7))))});
        }
        finally {
            for (long tid : new long[] {multi, cvlen, item, pt, nested, ivlen})
                if (tid >= 0)
                    H5.H5Tclose(tid);
            if (fid >= 0)
                H5.H5Fclose(fid);
        }
    }

    private static void write(long fid, String name, long tid, long[] dims, Object buf) throws Exception
    {
        long sid = -1, did = -1;
        try {
            sid = H5.H5Screate_simple(dims.length, dims, null);
            did = H5.H5Dcreate(fid, name, tid, sid, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT,
                               HDF5Constants.H5P_DEFAULT);
            if (buf instanceof Object[])
                H5.H5DwriteVL(did, tid, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                              HDF5Constants.H5P_DEFAULT, (Object[])buf);
            else
                H5.H5Dwrite(did, tid, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL,
                            HDF5Constants.H5P_DEFAULT, buf);
        }
        finally {
            if (did >= 0)
                H5.H5Dclose(did);
            if (sid >= 0)
                H5.H5Sclose(sid);
        }
    }

    @Test
    public void exportRegular()
    {
        exportAndCheck("regular", null, "1\t2\t3", "4\t5\t6");
    }

    /** Export keeps the default number format so the file can be imported again. */
    @Test
    public void exportRegularIgnoresHexDisplay()
    {
        exportAndCheck("regular", "Show Hexadecimal", "1\t2\t3", "4\t5\t6");
    }

    @Test
    public void exportNested()
    {
        exportAndCheck("nested", null, "1\t[10, 11]", "2\t[20]");
    }

    @Test
    public void exportMultiNested()
    {
        exportAndCheck("multi_nested", null, "1\t[{1, {2, 5}}, {3, {4, 6}}]", "2\t[{5, {6, 7}}]");
    }

    /**
     * Opens the dataset, optionally picks a Data Display menu item, exports through the table's
     * menu and compares the file line by line.
     */
    private void exportAndCheck(String datasetName, String displayMenuItem, String... expectedLines)
    {
        File exportFile = new File(workDir, datasetName + ".txt");
        exportFile.delete();

        SWTBotShell tableShell = null;
        File hdfFile           = openFile(FILENAME, FILE_MODE.READ_ONLY);

        try {
            tableShell = openTreeviewObject(bot.tree(), FILENAME, "/" + datasetName);
            if (displayMenuItem != null)
                tableShell.bot().menu().menu("Data Display").menu(displayMenuItem).click();

            tableShell.bot()
                .menu()
                .menu("Import/Export Data")
                .menu("Export Data to")
                .menu("Text File")
                .click();

            SWTBotShell nameShell = bot.shell("Enter a file name");
            nameShell.activate();
            nameShell.bot().text().setText(exportFile.getName());
            nameShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(nameShell));

            // The file is written on the UI thread after the dialog closes.
            bot.waitUntil(new DefaultCondition() {
                @Override
                public boolean test() throws Exception
                {
                    return exportFile.exists() && Files.readAllLines(exportFile.toPath()).size() >= expectedLines.length;
                }

                @Override
                public String getFailureMessage()
                {
                    return "export file " + exportFile + " was not written";
                }
            });

            List<String> lines = Files.readAllLines(exportFile.toPath());
            assertEquals(Arrays.asList(expectedLines), lines, datasetName + " export");
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
