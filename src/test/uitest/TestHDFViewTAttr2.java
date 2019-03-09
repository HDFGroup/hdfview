package test.uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Ignore;
import org.junit.Test;

import test.uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

public class TestHDFViewTAttr2 extends AbstractWindowTest {
    private static final String testFilename = "tattr2.h5";

    public void openTAttr2GroupTest(String testname, String datasetName, String[][] arrayExpectedData, String datasetName2, String[][] array2DExpectedData, String datasetName3,
            String[][] array3DPage1ExpectedData, String[][] array3DPage2ExpectedData) {
        SWTBotShell tableShell = null;
        File hdfFile = openFile(testFilename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, testname, 4, testFilename);

            // Open dataset 1D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName);
            SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname);

            retriever.testAllTableLocations(false, arrayExpectedData);

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Open dataset 2D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName2);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname);

            retriever.testAllTableLocations(false, array2DExpectedData);

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Open dataset 3D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName3);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname);

            retriever.testAllTableLocations(true, array3DPage1ExpectedData);

            tableShell.bot().toolbarButtonWithTooltip("Next Frame").click();

            retriever.testAllTableLocations(true, array3DPage2ExpectedData);
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
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void openTAttr2GroupCompoundTest(String testname, String datasetName, String[][] arrayExpectedData, String datasetName2, String[][] array2DExpectedData, String datasetName3,
            String[][] array3DPage1ExpectedData, String[][] array3DPage2ExpectedData) {
        SWTBotShell tableShell = null;
        File hdfFile = openFile(testFilename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, testname, 4, testFilename);

            // Open dataset 1D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName);
            SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname);

            retriever.setContainerHeaderOffset(2);
            retriever.testAllTableLocations(false, arrayExpectedData);

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Open dataset 2D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName2);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname);

            retriever.setContainerHeaderOffset(2);
            retriever.testAllTableLocations(false, array2DExpectedData);

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Open dataset 3D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName3);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname);

            retriever.setContainerHeaderOffset(2);
            retriever.testAllTableLocations(true, array3DPage1ExpectedData);

            tableShell.bot().toolbarButtonWithTooltip("Next Frame").click();

            retriever.testAllTableLocations(true, array3DPage2ExpectedData);
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
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openTAttr2GroupArray() {
        String[][] arrayExpectedData = {
                { "\\[1, 2, 3\\]" }, { "\\[4, 5, 6\\]" } };
        String[][] array2DExpectedData = {
                { "\\[1, 2, 3\\]", "\\[4, 5, 6\\]" },
                { "\\[7, 8, 9\\]", "\\[10, 11, 12\\]" },
                { "\\[13, 14, 15\\]", "\\[16, 17, 18\\]" } };
        String[][] array3DPage1ExpectedData = {
                { "\\[1, 2, 3\\]", "\\[7, 8, 9\\]", "\\[13, 14, 15\\]" },
                { "\\[19, 20, 21\\]", "\\[25, 26, 27\\]", "\\[31, 32, 33\\]" },
                { "\\[37, 38, 39\\]", "\\[43, 44, 45\\]", "\\[49, 50, 51\\]" },
                { "\\[55, 56, 57\\]", "\\[61, 62, 63\\]", "\\[67, 68, 69\\]" } };
        String[][] array3DPage2ExpectedData = {
                { "\\[4, 5, 6\\]", "\\[10, 11, 12\\]", "\\[16, 17, 18\\]" },
                { "\\[22, 23, 24\\]", "\\[28, 29, 30\\]", "\\[34, 35, 36\\]" },
                { "\\[40, 41, 42\\]", "\\[46, 47, 48\\]", "\\[52, 53, 54\\]" },
                { "\\[58, 59, 60\\]", "\\[64, 65, 66\\]", "\\[70, 71, 72\\]" } };
        String datasetg2Name = "/g2/array";
        String datasetg2Name2 = "/g2/array2D";
        String datasetg2Name3 = "/g2/array3D";

        openTAttr2GroupTest("openTAttr2GroupArray()", datasetg2Name, arrayExpectedData, datasetg2Name2, array2DExpectedData, datasetg2Name3, array3DPage1ExpectedData, array3DPage2ExpectedData);
    }

    @Test
    public void openTAttr2GroupBitfield() {
        String[][] arrayExpectedData = {
                { "01" }, { "02" } };
        String[][] array2DExpectedData = {
                { "01", "02" },
                { "03", "04" },
                { "05", "06" } };
        String[][] array3DPage1ExpectedData = {
                { "01", "03", "05" },
                { "07", "09", "0B" },
                { "0D", "0F", "11" },
                { "13", "15", "17" } };
        String[][] array3DPage2ExpectedData = {
                { "02", "04", "06" },
                { "08", "0A", "0C" },
                { "0E", "10", "12" },
                { "14", "16", "18" } };
        String datasetg2Name = "/g2/bitfield";
        String datasetg2Name2 = "/g2/bitfield2D";
        String datasetg2Name3 = "/g2/bitfield3D";

        openTAttr2GroupTest("openTAttr2GroupBitfield()", datasetg2Name, arrayExpectedData, datasetg2Name2, array2DExpectedData, datasetg2Name3, array3DPage1ExpectedData, array3DPage2ExpectedData);
    }

    @Test
    public void openTAttr2GroupCompound() {
        String[][] arrayExpectedData = {
                { "1", "2.0" },
                { "3", "4.0" } };
        String[][] array2DExpectedData = {
                { "1", "2.0", "3", "4.0" },
                { "5", "6.0", "7", "8.0" },
                { "9", "10.0","11", "12.0" } };
        String[][] array3DPage1ExpectedData = {
                { "1", "2.0", "5", "6.0", "9", "10.0" },
                { "13", "14.0", "17", "18.0", "21", "22.0" },
                { "25", "26.0", "29", "30.0", "33", "34.0" },
                { "37", "38.0", "41", "42.0", "45", "46.0" } };
        // TODO: update when paging fixed for compounds
        String[][] array3DPage2ExpectedData = {
                { "1", "2.0", "5", "6.0", "9", "10.0" },
                { "13", "14.0", "17", "18.0", "21", "22.0" },
                { "25", "26.0", "29", "30.0", "33", "34.0" },
                { "37", "38.0", "41", "42.0", "45", "46.0" } };
        String datasetg2Name = "/g2/compound";
        String datasetg2Name2 = "/g2/compound2D";
        String datasetg2Name3 = "/g2/compound3D";

        openTAttr2GroupCompoundTest("openTAttr2GroupCompound()", datasetg2Name, arrayExpectedData, datasetg2Name2, array2DExpectedData, datasetg2Name3, array3DPage1ExpectedData, array3DPage2ExpectedData);
    }

    @Test
    public void openTAttr2GroupEnum() {
        String[][] arrayExpectedData = {
                { "RED" }, { "RED" } };
        String[][] array2DExpectedData = {
                { "RED", "RED" },
                { "RED", "RED" },
                { "RED", "RED" } };
        String[][] array3DPage1ExpectedData = {
                { "RED", "RED", "RED" },
                { "RED", "RED", "RED" },
                { "RED", "RED", "RED" },
                { "RED", "RED", "RED" } };
        String[][] array3DPage2ExpectedData = {
                { "RED", "RED", "RED" },
                { "RED", "RED", "RED" },
                { "RED", "RED", "RED" },
                { "RED", "RED", "RED" } };
        String datasetg2Name = "/g2/enum";
        String datasetg2Name2 = "/g2/enum2D";
        String datasetg2Name3 = "/g2/enum3D";

        openTAttr2GroupTest("openTAttr2GroupEnum()", datasetg2Name, arrayExpectedData, datasetg2Name2, array2DExpectedData, datasetg2Name3, array3DPage1ExpectedData, array3DPage2ExpectedData);
    }

    @Test
    public void openTAttr2GroupFloat() {
        String[][] arrayExpectedData = {
                { "1.0" }, { "2.0" } };
        String[][] array2DExpectedData = {
                { "1.0", "2.0" },
                { "3.0", "4.0" },
                { "5.0", "6.0" } };
        String[][] array3DPage1ExpectedData = {
                { "1.0", "3.0", "5.0" },
                { "7.0", "9.0", "11.0" },
                { "13.0", "15.0", "17.0" },
                { "19.0", "21.0", "23.0" } };
        String[][] array3DPage2ExpectedData = {
                { "2.0", "4.0", "6.0" },
                { "8.0", "10.0", "12.0" },
                { "14.0", "16.0", "18.0" },
                { "20.0", "22.0", "24.0" } };
        String datasetg2Name = "/g2/float";
        String datasetg2Name2 = "/g2/float2D";
        String datasetg2Name3 = "/g2/float3D";

        openTAttr2GroupTest("openTAttr2GroupFloat()", datasetg2Name, arrayExpectedData, datasetg2Name2, array2DExpectedData, datasetg2Name3, array3DPage1ExpectedData, array3DPage2ExpectedData);
    }

    @Test
    public void openTAttr2GroupInteger() {
        String[][] arrayExpectedData = {
                { "1" }, { "2" } };
        String[][] array2DExpectedData = {
                { "1", "2" },
                { "3", "4" },
                { "5", "6" } };
        String[][] array3DPage1ExpectedData = {
                { "1", "3", "5" },
                { "7", "9", "11" },
                { "13", "15", "17" },
                { "19", "21", "23" } };
        String[][] array3DPage2ExpectedData = {
                { "2", "4", "6" },
                { "8", "10", "12" },
                { "14", "16", "18" },
                { "20", "22", "24" } };
        String datasetg2Name = "/g2/integer";
        String datasetg2Name2 = "/g2/integer2D";
        String datasetg2Name3 = "/g2/integer3D";

        openTAttr2GroupTest("openTAttr2GroupInteger()", datasetg2Name, arrayExpectedData, datasetg2Name2, array2DExpectedData, datasetg2Name3, array3DPage1ExpectedData, array3DPage2ExpectedData);
    }

    @Test
    public void openTAttr2GroupOpaque() {
        String[][] arrayExpectedData = {
                { "01" }, { "02" } };
        String[][] array2DExpectedData = {
                { "01", "02" },
                { "03", "04" },
                { "05", "06" } };
        String[][] array3DPage1ExpectedData = {
                { "01", "03", "05" },
                { "07", "09", "0B" },
                { "0D", "0F", "11" },
                { "13", "15", "17" } };
        String[][] array3DPage2ExpectedData = {
                { "02", "04", "06" },
                { "08", "0A", "0C" },
                { "0E", "10", "12" },
                { "14", "16", "18" } };
        String datasetg2Name = "/g2/opaque";
        String datasetg2Name2 = "/g2/opaque2D";
        String datasetg2Name3 = "/g2/opaque3D";

        openTAttr2GroupTest("openTAttr2GroupOpaque()", datasetg2Name, arrayExpectedData, datasetg2Name2, array2DExpectedData, datasetg2Name3, array3DPage1ExpectedData, array3DPage2ExpectedData);
    }

    @Test
    public void openTAttr2GroupReference() {
        String[][] arrayExpectedData = {
                { "/dset" }, { "/dset" } };
        String[][] array2DExpectedData = {
                { "/dset", "/dset" },
                { "/dset", "/dset" },
                { "/dset", "/dset" } };
        String[][] array3DPage1ExpectedData = {
                { "/dset", "/dset", "/dset" },
                { "/dset", "/dset", "/dset" },
                { "/dset", "/dset", "/dset" },
                { "/dset", "/dset", "/dset" } };
        String[][] array3DPage2ExpectedData = {
                { "/dset", "/dset", "/dset" },
                { "/dset", "/dset", "/dset" },
                { "/dset", "/dset", "/dset" },
                { "/dset", "/dset", "/dset" } };
        String datasetg2Name = "/g2/reference";
        String datasetg2Name2 = "/g2/reference2D";
        String datasetg2Name3 = "/g2/reference3D";

        openTAttr2GroupTest("openTAttr2GroupReference()", datasetg2Name, arrayExpectedData, datasetg2Name2, array2DExpectedData, datasetg2Name3, array3DPage1ExpectedData, array3DPage2ExpectedData);
    }

    @Test
    public void openTAttr2GroupString() {
        String[][] arrayExpectedData = {
                { "ab" }, { "de" } };
        String[][] array2DExpectedData = {
                { "ab", "cd" },
                { "ef", "gh" },
                { "ij", "kl" } };
        String[][] array3DPage1ExpectedData = {
                { "ab", "ef", "ij" },
                { "mn", "rs", "vw" },
                { "AB", "EF", "IJ" },
                { "MN", "RS", "VW" } };
        // TODO: update when paging fixed for strings
        String[][] array3DPage2ExpectedData = {
                { "ab", "ef", "ij" },
                { "mn", "rs", "vw" },
                { "AB", "EF", "IJ" },
                { "MN", "RS", "VW" } };
        String datasetg2Name = "/g2/string";
        String datasetg2Name2 = "/g2/string2D";
        String datasetg2Name3 = "/g2/string3D";

        openTAttr2GroupTest("openTAttr2GroupString()", datasetg2Name, arrayExpectedData, datasetg2Name2, array2DExpectedData, datasetg2Name3, array3DPage1ExpectedData, array3DPage2ExpectedData);
    }

    @Ignore
    public void openTAttr2GroupVlen() {
        String[][] arrayExpectedData = {
                { "\\(1\\)" }, { "\\(2, 3\\)" } };
        String[][] array2DExpectedData = {
                { "\\(0\\)", "\\(1\\)" },
                { "\\(2, 3\\)", "\\(4, 5\\)" },
                { "\\(6, 7, 8\\)", "\\(9, 10, 11\\)" } };
        String[][] array3DPage1ExpectedData = {
                { "\\(0\\)", "\\(2\\)", "\\(4\\)" },
                { "\\(6, 7\\)", "\\(10, 11\\)", "\\(14, 15\\)" },
                { "\\(18, 19, 20\\)", "\\(24, 25, 26\\)", "\\(30, 31, 32\\)" },
                { "\\(36, 37, 38, 39\\)", "\\(44, 45, 46, 47\\)", "\\(52, 53, 54, 55\\)" } };
        String[][] array3DPage2ExpectedData = {
                { "\\(1\\)", "\\(3\\)", "\\(5\\)" },
                { "\\(8, 9\\)", "\\(12, 13\\)", "\\(16, 17\\)" },
                { "\\(21, 22, 23\\)", "\\(27, 28, 29\\)", "\\(33, 34, 35\\)" },
                { "\\(40, 41, 42, 43\\)", "\\(48, 49, 50, 51\\)", "\\(56, 57, 58, 59\\)" } };
        String datasetg2Name = "/g2/vlen";
        String datasetg2Name2 = "/g2/vlen2D";
        String datasetg2Name3 = "/g2/vlen3D";

        openTAttr2GroupTest("openTAttr2GroupVlen()", datasetg2Name, arrayExpectedData, datasetg2Name2, array2DExpectedData, datasetg2Name3, array3DPage1ExpectedData, array3DPage2ExpectedData);
    }

    @Test
    public void openTAttr2Attribute() {
        String[][] expectedAttrData = {
                { "array", "Array \\[3\\] of 32-bit integer", "2", "1, 2, 3, 4, 5, 6" },
                { "array2D", "Array \\[3\\] of 32-bit integer", "3 x 2", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18" },
                { "array3D", "Array \\[3\\] of 32-bit integer", "4 x 3 x 2", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50" },
                { "bitfield", "8-bit bitfield", "2", "1, 2" },
                { "bitfield2D", "8-bit bitfield", "3 x 2", "1, 2, 3, 4, 5, 6" },
                { "bitfield3D", "8-bit bitfield", "4 x 3 x 2", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24" },
                { "compound", "Compound \\{a = 8-bit integer, b = 64-bit floating-point\\}", "2", "\\{1, 2\\}, \\{3, 4\\}" },
                { "compound2D", "Compound \\{a = 8-bit integer, b = 64-bit floating-point\\}", "3 x 2", "\\{1, 2\\}, \\{3, 4\\}, \\{5, 6\\}, \\{7, 8\\}, \\{9, 10\\}, \\{11, 12\\}" },
                { "compound3D", "Compound \\{a = 8-bit integer, b = 64-bit floating-point\\}", "4 x 3 x 2", "\\{1, 2\\}, \\{3, 4\\}, \\{5, 6\\}, \\{7, 8\\}, \\{9, 10\\}, \\{11, 12\\}, \\{13, 14\\}, \\{15, 16\\}, \\{17, 18\\}, \\{19, 20\\}, \\{21, 22\\}, \\{23, 24\\}, \\{25, 26\\}, \\{27, 28\\}, \\{29, 30\\}, \\{31, 32\\}, \\{33, 34\\}, \\{35, 36\\}, \\{37, 38\\}, \\{39, 40\\}, \\{41, 42\\}, \\{43, 44\\}, \\{45, 46\\}, \\{47, 48\\}" },
                { "enum", "32-bit enum \\(0=RED, 1=GREEN\\)", "2", "RED, RED" },
                { "enum2D", "32-bit enum \\(0=RED, 1=GREEN\\)", "3 x 2", "RED, RED, RED, RED, RED, RED" },
                { "enum3D", "32-bit enum \\(0=RED, 1=GREEN\\)", "4 x 3 x 2", "RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED" },
                { "float", "32-bit floating-point", "2", "1.0, 2.0" },
                { "float2D", "32-bit floating-point", "3 x 2", "1.0, 2.0, 3.0, 4.0, 5.0, 6.0" },
                { "float3D", "32-bit floating-point", "4 x 3 x 2", "1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0" },
                { "integer", "32-bit integer", "2", "1, 2" },
                { "integer2D", "32-bit integer", "3 x 2", "1, 2, 3, 4, 5, 6" },
                { "integer3D", "32-bit integer", "4 x 3 x 2", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24" },
                { "opaque", "1-byte Opaque, tag = 1-byte opaque type", "2", "1, 2" },
                { "opaque2D", "1-byte Opaque, tag = 1-byte opaque type", "3 x 2", "1, 2, 3, 4, 5, 6" },
                { "opaque3D", "1-byte Opaque, tag = 1-byte opaque type", "4 x 3 x 2", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24" },
                { "reference", "Object reference", "2", "976, 976" },
                { "reference2D", "Object reference", "3 x 2", "976, 976, 976, 976, 976, 976" },
                { "reference3D", "Object reference", "4 x 3 x 2", "976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976" },
                { "string", "String, length = 2, padding = H5T_STR_NULLTERM, cset = H5T_CSET_ASCII", "2", "ab, de" },
                { "string2D", "String, length = 2, padding = H5T_STR_NULLTERM, cset = H5T_CSET_ASCII", "3 x 2", "ab, cd, ef, gh, ij, kl" },
                { "string3D", "String, length = 2, padding = H5T_STR_NULLTERM, cset = H5T_CSET_ASCII", "4 x 3 x 2", "ab, cd, ef, gh, ij, kl, mn, pq, rs, tu, vw, xz, AB, CD, EF, GH, IJ, KL, MN, PQ, RS, TU, VW, XZ" },
                { "vlen", "Variable-length of 32-bit integer", "2", "\\(1\\), \\(2, 3\\)" },
                { "vlen2D", "Variable-length of 32-bit integer", "3 x 2", "\\(0\\), \\(1\\), \\(2, 3\\), \\(4, 5\\), \\(6, 7, 8\\), \\(9, 10, 11\\)" },
                { "vlen3D", "Variable-length of 32-bit integer", "4 x 3 x 2", "\\(0\\), \\(1\\), \\(2\\), \\(3\\), \\(4\\), \\(5\\), \\(6, 7\\), \\(8, 9\\), \\(10, 11\\), \\(12, 13\\), \\(14, 15\\), \\(16, 17\\), \\(18, 19, 20\\), \\(21, 22, 23\\), \\(24, 25, 26\\), \\(27, 28, 29\\), \\(30, 31, 32\\), \\(33, 34, 35\\), \\(36, 37, 38, 39\\), \\(40, 41, 42, 43\\), \\(44, 45, 46, 47\\), \\(48, 49, 50, 51\\), \\(52, 53, 54, 55\\), \\(56, 57, 58, 59\\)" },
                };
        String datasetName = "dset";
        File hdfFile = openFile(testFilename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "openTAttr2Attribute()", 4, testFilename);

            // Open dataset Attribute Table
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, datasetName);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(attrTable, "openTAttr2Attribute()");

            retriever.testAllTableLocations(false, expectedAttrData);
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
            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Ignore
    public void openTAttr2GroupReferenceAsTable() {
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2Name3 = "reference3D";
        SWTBotShell tableShell = null;
        SWTBotShell table2Shell = null;
        File hdf_file = openFile(testFilename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupReferenceAsTable()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupReferenceAsTable() filetree is missing file '" + testFilename + "'", items[0].getText().compareTo(testFilename)==0);
            assertTrue("openTAttr2GroupReferenceAsTable() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupReferenceAsTable() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupReferenceAsTable() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(23).click();
            items[0].getNode(2).getNode(23).contextMenu().menu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2Name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 3);
            assertTrue("openTAttr2GroupReferenceAsTable() data ["+tableShell.bot().text(2).getText()+"] did not match regex '/dset'",
                    tableShell.bot().text(2).getText().matches("/dset"));

            table.contextMenu(3, 3).menu("Show As &Table").click();
            org.hamcrest.Matcher<Shell> shell2Matcher = WithRegex.withRegex(dataset_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shell2Matcher));

            table2Shell = bot.shells()[2];
            table2Shell.activate();
            bot.waitUntil(Conditions.shellIsActive(table2Shell.getText()));

            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            SWTBotNatTable table2 = new SWTBotNatTable(table2Shell.bot().widget(widgetOfType(NatTable.class)));

            table2.click(2, 1);
            assertTrue("openTAttr2GroupReferenceAsTable() data ["+table2Shell.bot().text(0).getText()+"] did not match regex '0'",
                    table2Shell.bot().text(0).getText().matches("0"));
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
            if(table2Shell != null && table2Shell.isOpen()) {
                table2Shell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(table2Shell));
            }

            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
