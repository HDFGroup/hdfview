package uitest;

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

import uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

public class TestHDFViewTAttr2 extends AbstractWindowTest {
    private static final String testFilename = "tattr2.h5";
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
    String[][] bitfieldExpectedData = {
            { "01" }, { "02" } };
    String[][] bitfield2DExpectedData = {
            { "01", "02" },
            { "03", "04" },
            { "05", "06" } };
    String[][] bitfield3DPage1ExpectedData = {
            { "01", "03", "05" },
            { "07", "09", "0B" },
            { "0D", "0F", "11" },
            { "13", "15", "17" } };
    String[][] bitfield3DPage2ExpectedData = {
            { "02", "04", "06" },
            { "08", "0A", "0C" },
            { "0E", "10", "12" },
            { "14", "16", "18" } };
    String[][] compoundExpectedData = {
            { "1", "2.0" },
            { "3", "4.0" } };
    String[][] compound2DExpectedData = {
            { "1", "2.0", "3", "4.0" },
            { "5", "6.0", "7", "8.0" },
            { "9", "10.0","11", "12.0" } };
    String[][] compound3DPage1ExpectedData = {
            { "1", "2.0", "5", "6.0", "9", "10.0" },
            { "13", "14.0", "17", "18.0", "21", "22.0" },
            { "25", "26.0", "29", "30.0", "33", "34.0" },
            { "37", "38.0", "41", "42.0", "45", "46.0" } };
    String[][] compound3DPage2ExpectedData = {
            { "3", "4.0", "7", "8.0", "11", "12.0" },
            { "15", "16.0", "19", "20.0", "23", "24.0" },
            { "27", "28.0", "31", "32.0", "35", "36.0" },
            { "39", "40.0", "43", "44.0", "47", "48.0" } };
    String[][] enumExpectedData = {
            { "RED" }, { "RED" } };
    String[][] enum2DExpectedData = {
            { "RED", "RED" },
            { "RED", "RED" },
            { "RED", "RED" } };
    String[][] enum3DPage1ExpectedData = {
            { "RED", "RED", "RED" },
            { "RED", "RED", "RED" },
            { "RED", "RED", "RED" },
            { "RED", "RED", "RED" } };
    String[][] enum3DPage2ExpectedData = {
            { "RED", "RED", "RED" },
            { "RED", "RED", "RED" },
            { "RED", "RED", "RED" },
            { "RED", "RED", "RED" } };
    String[][] floatExpectedData = {
            { "1.0" }, { "2.0" } };
    String[][] float2DExpectedData = {
            { "1.0", "2.0" },
            { "3.0", "4.0" },
            { "5.0", "6.0" } };
    String[][] float3DPage1ExpectedData = {
            { "1.0", "3.0", "5.0" },
            { "7.0", "9.0", "11.0" },
            { "13.0", "15.0", "17.0" },
            { "19.0", "21.0", "23.0" } };
    String[][] float3DPage2ExpectedData = {
            { "2.0", "4.0", "6.0" },
            { "8.0", "10.0", "12.0" },
            { "14.0", "16.0", "18.0" },
            { "20.0", "22.0", "24.0" } };
    String[][] integerExpectedData = {
            { "1" }, { "2" } };
    String[][] integer2DExpectedData = {
            { "1", "2" },
            { "3", "4" },
            { "5", "6" } };
    String[][] integer3DPage1ExpectedData = {
            { "1", "3", "5" },
            { "7", "9", "11" },
            { "13", "15", "17" },
            { "19", "21", "23" } };
    String[][] integer3DPage2ExpectedData = {
            { "2", "4", "6" },
            { "8", "10", "12" },
            { "14", "16", "18" },
            { "20", "22", "24" } };
    String[][] opaqueExpectedData = {
            { "01" }, { "02" } };
    String[][] opaque2DExpectedData = {
            { "01", "02" },
            { "03", "04" },
            { "05", "06" } };
    String[][] opaque3DPage1ExpectedData = {
            { "01", "03", "05" },
            { "07", "09", "0B" },
            { "0D", "0F", "11" },
            { "13", "15", "17" } };
    String[][] opaque3DPage2ExpectedData = {
            { "02", "04", "06" },
            { "08", "0A", "0C" },
            { "0E", "10", "12" },
            { "14", "16", "18" } };
    String[][] referenceExpectedData = {
            { "/dset H5O_TYPE_OBJ_REF" }, { "/dset H5O_TYPE_OBJ_REF" } };
    String[][] reference2DExpectedData = {
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" },
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" },
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" } };
    String[][] reference3DPage1ExpectedData = {
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" },
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" },
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" },
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" } };
    String[][] reference3DPage2ExpectedData = {
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" },
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" },
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" },
            { "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF", "/dset H5O_TYPE_OBJ_REF" } };
    String[][] stringExpectedData = {
            { "ab" }, { "de" } };
    String[][] string2DExpectedData = {
            { "ab", "cd" },
            { "ef", "gh" },
            { "ij", "kl" } };
    String[][] string3DPage1ExpectedData = {
            { "ab", "ef", "ij" },
            { "mn", "rs", "vw" },
            { "AB", "EF", "IJ" },
            { "MN", "RS", "VW" } };
    String[][] string3DPage2ExpectedData = {
            { "cd", "gh", "kl" },
            { "pq", "tu", "xz" },
            { "CD", "GH", "KL" },
            { "PQ", "TU", "XZ" } };
    String[][] vlenExpectedData = {
            { "\\[1\\]" }, { "\\[2, 3\\]" } };
    String[][] vlen2DExpectedData = {
            { "\\[0\\]", "\\[1\\]" },
            { "\\[2, 3\\]", "\\[4, 5\\]" },
            { "\\[6, 7, 8\\]", "\\[9, 10, 11\\]" } };
    String[][] vlen3DPage1ExpectedData = {
            { "\\[0\\]", "\\[2\\]", "\\[4\\]" },
            { "\\[6, 7\\]", "\\[10, 11\\]", "\\[14, 15\\]" },
            { "\\[18, 19, 20\\]", "\\[24, 25, 26\\]", "\\[30, 31, 32\\]" },
            { "\\[36, 37, 38, 39\\]", "\\[44, 45, 46, 47\\]", "\\[52, 53, 54, 55\\]" } };
    String[][] vlen3DPage2ExpectedData = {
            { "\\[1\\]", "\\[3\\]", "\\[5\\]" },
            { "\\[8, 9\\]", "\\[12, 13\\]", "\\[16, 17\\]" },
            { "\\[21, 22, 23\\]", "\\[27, 28, 29\\]", "\\[33, 34, 35\\]" },
            { "\\[40, 41, 42, 43\\]", "\\[48, 49, 50, 51\\]", "\\[56, 57, 58, 59\\]" } };

    private void openTAttr2GroupTest(SWTBotTree filetree, String testname, String datasetName, String[][] testExpectedData, String datasetName2, String[][] test2DExpectedData, String datasetName3,
            String[][] test3DPage1ExpectedData, String[][] test3DPage2ExpectedData)
    {
        SWTBotShell tableShell = null;
        try {
            // Open dataset 1D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName);
            SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);

            retriever.testAllTableLocations(testExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Open dataset 2D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName2);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);

            retriever.testAllTableLocations(test2DExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Open dataset 3D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName3);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);
            retriever.setPagingActive(true);

            retriever.testAllTableLocations(test3DPage1ExpectedData);

            tableShell.bot().toolbarButtonWithTooltip("Next Frame").click();

            retriever.testAllTableLocations(test3DPage2ExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
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
        }
    }

    private void openTAttr2GroupCompoundTest(SWTBotTree filetree, String testname, String datasetName, String[][] testExpectedData, String datasetName2, String[][] test2DExpectedData, String datasetName3,
            String[][] test3DPage1ExpectedData, String[][] test3DPage2ExpectedData)
    {
        SWTBotShell tableShell = null;

        try {
            // Open dataset 1D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName);
            SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);

            retriever.setContainerHeaderOffset(2, 0);
            retriever.testAllTableLocations(testExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Open dataset 2D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName2);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);

            retriever.setContainerHeaderOffset(2, 0);
            retriever.testAllTableLocations(test2DExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Open dataset 3D
            tableShell = openTreeviewObject(filetree, testFilename, datasetName3);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);
            retriever.setPagingActive(true);

            retriever.setContainerHeaderOffset(2, 0);
            retriever.testAllTableLocations(test3DPage1ExpectedData);

            tableShell.bot().toolbarButtonWithTooltip("Next Frame").click();

            retriever.testAllTableLocations(test3DPage2ExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
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
        }
    }

    @Test
    public void datasetTAttr2GroupTest()
    {
        File hdfFile = openFile(testFilename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "datasetTAttr2GroupTest()", 4, testFilename);

            openTAttr2GroupTest(filetree, "datasetTAttr2GroupTest()", "/g2/array", arrayExpectedData, "/g2/array2D", array2DExpectedData, "/g2/array3D", array3DPage1ExpectedData, array3DPage2ExpectedData);
            openTAttr2GroupTest(filetree, "datasetTAttr2GroupTest()", "/g2/bitfield", bitfieldExpectedData, "/g2/bitfield2D", bitfield2DExpectedData, "/g2/bitfield3D", bitfield3DPage1ExpectedData, bitfield3DPage2ExpectedData);
            openTAttr2GroupCompoundTest(filetree, "datasetTAttr2GroupTest()", "/g2/compound", compoundExpectedData, "/g2/compound2D", compound2DExpectedData, "/g2/compound3D", compound3DPage1ExpectedData, compound3DPage2ExpectedData);
            // TODO: unable to enable 'convert enum' option from SWTBot
            // openTAttr2GroupTest(filetree, "datasetTAttr2GroupTest()", "/g2/enum", enumExpectedData, "/g2/enum2D", enum2DExpectedData, "/g2/enum3D", enum3DPage1ExpectedData, enum3DPage2ExpectedData);
            openTAttr2GroupTest(filetree, "datasetTAttr2GroupTest()", "/g2/float", floatExpectedData, "/g2/float2D", float2DExpectedData, "/g2/float3D", float3DPage1ExpectedData, float3DPage2ExpectedData);
            openTAttr2GroupTest(filetree, "datasetTAttr2GroupTest()", "/g2/integer", integerExpectedData, "/g2/integer2D", integer2DExpectedData, "/g2/integer3D", integer3DPage1ExpectedData, integer3DPage2ExpectedData);
            openTAttr2GroupTest(filetree, "datasetTAttr2GroupTest()", "/g2/opaque", opaqueExpectedData, "/g2/opaque2D", opaque2DExpectedData, "/g2/opaque3D", opaque3DPage1ExpectedData, opaque3DPage2ExpectedData);
            openTAttr2GroupTest(filetree, "datasetTAttr2GroupTest()", "/g2/reference", referenceExpectedData, "/g2/reference2D", reference2DExpectedData, "/g2/reference3D", reference3DPage1ExpectedData, reference3DPage2ExpectedData);
            openTAttr2GroupTest(filetree, "datasetTAttr2GroupTest()", "/g2/string", stringExpectedData, "/g2/string2D", string2DExpectedData, "/g2/string3D", string3DPage1ExpectedData, string3DPage2ExpectedData);
            openTAttr2GroupTest(filetree, "datasetTAttr2GroupTest()", "/g2/vlen", vlenExpectedData, "/g2/vlen2D", vlen2DExpectedData, "/g2/vlen3D", vlen3DPage1ExpectedData, vlen3DPage2ExpectedData);
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

    private void openTAttr2AttributeTest(SWTBotTable attrTable, int rowIndex, String testname, String attrName, String[][] testExpectedData, String attrName2, String[][] test2DExpectedData, String attrName3,
            String[][] test3DPage1ExpectedData, String[][] test3DPage2ExpectedData)
    {
        SWTBotShell tableShell = null;
        try {
            // Open attribute 1D
            tableShell = openAttributeObject(attrTable, attrName, rowIndex);
            SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);

            retriever.testAllTableLocations(testExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Open attribute 2D
            tableShell = openAttributeObject(attrTable, attrName2, rowIndex+1);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);

            retriever.testAllTableLocations(test2DExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            //TODO: attribute 3D tables are different and don't page
            // Open attribute 3D
            tableShell = openAttributeObject(attrTable, attrName3, rowIndex+2);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);
            retriever.setPagingActive(true);

            //retriever.testAllTableLocations(test3DPage1ExpectedData);

            tableShell.bot().toolbarButtonWithTooltip("Next Frame").click();

            //retriever.testAllTableLocations(test3DPage2ExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
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
        }
    }

    private void openTAttr2AttributeCompoundTest(SWTBotTable attrTable, int rowIndex, String testname, String attrName, String[][] testExpectedData, String attrName2, String[][] test2DExpectedData, String attrName3,
            String[][] test3DPage1ExpectedData, String[][] test3DPage2ExpectedData)
    {
        SWTBotShell tableShell = null;

        try {
            // Open attribute 1D
            tableShell = openAttributeObject(attrTable, attrName, rowIndex);
            SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);

            retriever.setContainerHeaderOffset(2, 0);
            retriever.testAllTableLocations(testExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Open attribute 2D
            tableShell = openAttributeObject(attrTable, attrName2, rowIndex+1);
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);

            retriever.setContainerHeaderOffset(2, 0);
            retriever.testAllTableLocations(test2DExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            //TODO: attribute 3D compound tables are different and don't page
            // Open attribute 3D

            tableShell = openAttributeObject(attrTable, attrName3, rowIndex+2); dataTable =
            getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, testname, false);
            retriever.setPagingActive(true);

            retriever.setContainerHeaderOffset(2, 0);
            //retriever.testAllTableLocations(test3DPage1ExpectedData);

            tableShell.bot().toolbarButtonWithTooltip("Next Frame").click();

            //retriever.testAllTableLocations(test3DPage2ExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
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
        }
    }

    @Test
    public void openTAttr2Attribute()
    {
        String[][] expectedAttrData = {
                { "array", "Array \\[3\\] of 32-bit integer", "2", "1, 2, 3, 4, 5, 6" },
                { "array2D", "Array \\[3\\] of 32-bit integer", "3 x 2", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18" },
                { "array3D", "Array \\[3\\] of 32-bit integer", "4 x 3 x 2", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50" },
                { "bitfield", "8-bit bitfield", "2", "1, 2" },
                { "bitfield2D", "8-bit bitfield", "3 x 2", "1, 2, 3, 4, 5, 6" },
                { "bitfield3D", "8-bit bitfield", "4 x 3 x 2", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24" },
                { "compound", "Compound \\{a = 8-bit integer, b = 64-bit floating-point\\}", "2", "\\{1, 3, 2.0, 4.\\}" },
                { "compound2D", "Compound \\{a = 8-bit integer, b = 64-bit floating-point\\}", "3 x 2", "\\{1, 3, 5, 7, 9, 11, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0\\}" },
                { "compound3D", "Compound \\{a = 8-bit integer, b = 64-bit floating-point\\}", "4 x 3 x 2", "\\{1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35, 37, 39, 41, 43, 45, 47, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0, 26.0, 28.0, 30.0, 32.0, 34.0, 36.0, 38.0, 40.0, 42.0, 44.0, 46.0, 48.0\\}" },
                { "enum", "32-bit enum \\(0=RED, 1=GREEN\\)", "2", "RED, RED" },
                { "enum2D", "32-bit enum \\(0=RED, 1=GREEN\\)", "3 x 2", "RED, RED, RED, RED, RED, RED" },
                { "enum3D", "32-bit enum \\(0=RED, 1=GREEN\\)", "4 x 3 x 2", "RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED" },
                { "float", "32-bit floating-point", "2", "1.0, 2." },
                { "float2D", "32-bit floating-point", "3 x 2", "1.0, 2.0, 3.0, 4.0, 5.0, 6.0" },
                { "float3D", "32-bit floating-point", "4 x 3 x 2", "1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0" },
                { "integer", "32-bit integer", "2", "1, 2" },
                { "integer2D", "32-bit integer", "3 x 2", "1, 2, 3, 4, 5, 6" },
                { "integer3D", "32-bit integer", "4 x 3 x 2", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24" },
                { "opaque", "1-byte Opaque, tag = 1-byte opaque type", "2", "1, 2" },
                { "opaque2D", "1-byte Opaque, tag = 1-byte opaque type", "3 x 2", "1, 2, 3, 4, 5, 6" },
                { "opaque3D", "1-byte Opaque, tag = 1-byte opaque type", "4 x 3 x 2", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24" },
                { "reference", "Object reference", "2", "/dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF" },
                { "reference2D", "Object reference", "3 x 2", "/dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF" },
                { "reference3D", "Object reference", "4 x 3 x 2", "/dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF, /dset H5O_TYPE_OBJ_REF" },
                { "string", "String, length = 2, padding = H5T_STR_NULLTERM, cset = H5T_CSET_ASCII", "2", "ab, de" },
                { "string2D", "String, length = 2, padding = H5T_STR_NULLTERM, cset = H5T_CSET_ASCII", "3 x 2", "ab, cd, ef, gh, ij, kl" },
                { "string3D", "String, length = 2, padding = H5T_STR_NULLTERM, cset = H5T_CSET_ASCII", "4 x 3 x 2", "ab, cd, ef, gh, ij, kl, mn, pq, rs, tu, vw, xz, AB, CD, EF, GH, IJ, KL, MN, PQ, RS, TU, VW, XZ" },
                { "vlen", "Variable-length of 32-bit integer", "2", "\\[1\\], \\[2, 3\\]" },
                { "vlen2D", "Variable-length of 32-bit integer", "3 x 2", "\\[0\\], \\[1\\], \\[2, 3\\], \\[4, 5\\], \\[6, 7, 8\\], \\[9, 10, 11\\]" },
                { "vlen3D", "Variable-length of 32-bit integer", "4 x 3 x 2", "\\[0\\], \\[1\\], \\[2\\], \\[3\\], \\[4\\], \\[5\\], \\[6, 7\\], \\[8, 9\\], \\[10, 11\\], \\[12, 13\\], \\[14, 15\\], \\[16, 17\\], \\[18, 19, 20\\], \\[21, 22, 23\\], \\[24, 25, 26\\], \\[27, 28, 29\\], \\[30, 31, 32\\], \\[33, 34, 35\\], \\[36, 37, 38, 39\\], \\[40, 41, 42, 43\\], \\[44, 45, 46, 47\\], \\[48, 49, 50, 51\\], \\[52, 53, 54, 55\\], \\[56, 57, 58, 59\\]" },
                };
        String datasetName = "dset";
        File hdfFile = openFile(testFilename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "openTAttr2Attribute()", 4, testFilename);

            // Open dataset Attribute Table
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, datasetName);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(attrTable, "openTAttr2Attribute()", false);

            retriever.testAllTableLocations(expectedAttrData);

            openTAttr2AttributeTest(attrTable, 0, "openTAttr2Attribute()", "array", arrayExpectedData,  "array2D", array2DExpectedData,  "array3D", array3DPage1ExpectedData, array3DPage2ExpectedData);
            openTAttr2AttributeTest(attrTable, 3, "openTAttr2Attribute()", "bitfield", bitfieldExpectedData,  "bitfield2D", bitfield2DExpectedData,  "bitfield3D", bitfield3DPage1ExpectedData, bitfield3DPage2ExpectedData);
            openTAttr2AttributeCompoundTest(attrTable, 6, "openTAttr2Attribute()", "compound", compoundExpectedData,  "compound2D", compound2DExpectedData,  "compound3D", compound3DPage1ExpectedData, compound3DPage2ExpectedData);
            // TODO: unable to enable 'convert enum' option from SWTBot
            // openTAttr2AttributeTest(attrTable, 9, "openTAttr2Attribute()", "enum", enumExpectedData,  "enum2D", enum2DExpectedData,  "enum3D", enum3DPage1ExpectedData, enum3DPage2ExpectedData);
            openTAttr2AttributeTest(attrTable, 12, "openTAttr2Attribute()", "float", floatExpectedData,  "float2D", float2DExpectedData,  "float3D", float3DPage1ExpectedData, float3DPage2ExpectedData);
            openTAttr2AttributeTest(attrTable, 15, "openTAttr2Attribute()", "integer", integerExpectedData,  "integer2D", integer2DExpectedData,  "integer3D", integer3DPage1ExpectedData, integer3DPage2ExpectedData);
            openTAttr2AttributeTest(attrTable, 18, "openTAttr2Attribute()", "opaque", opaqueExpectedData,  "opaque2D", opaque2DExpectedData,  "opaque3D", opaque3DPage1ExpectedData, opaque3DPage2ExpectedData);
            openTAttr2AttributeTest(attrTable, 21, "openTAttr2Attribute()", "reference", referenceExpectedData,  "reference2D", reference2DExpectedData,  "reference3D", reference3DPage1ExpectedData, reference3DPage2ExpectedData);
            openTAttr2AttributeTest(attrTable, 24, "openTAttr2Attribute()", "string", stringExpectedData,  "string2D", string2DExpectedData,  "string3D", string3DPage1ExpectedData, string3DPage2ExpectedData);
            openTAttr2AttributeTest(attrTable, 27, "openTAttr2Attribute()", "vlen", vlenExpectedData,  "vlen2D", vlen2DExpectedData,  "vlen3D", vlen3DPage1ExpectedData, vlen3DPage2ExpectedData);
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
    public void openTAttr2GroupReferenceAsTable()
    {
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
            items[0].getNode(0).contextMenu().contextMenu("Expand All").click();

            items[0].getNode(2).getNode(23).click();
            items[0].getNode(2).getNode(23).contextMenu().menu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2Name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 3);
            assertTrue("openTAttr2GroupReferenceAsTable() data ["+tableShell.bot().text(2).getText()+"] did not match regex '/dset H5O_TYPE_OBJ_REF'",
                    tableShell.bot().text(2).getText().matches("/dset H5O_TYPE_OBJ_REF"));

            table.contextMenu(3, 3).menu("Show As &Table").click();
            org.hamcrest.Matcher<Shell> shell2Matcher = WithRegex.withRegex(dataset_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shell2Matcher));

            table2Shell = bot.shells()[2];
            table2Shell.activate();
            bot.waitUntil(Conditions.shellIsActive(table2Shell.getText()));

            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
            closeShell(table2Shell);
            closeShell(tableShell);

            try {
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
