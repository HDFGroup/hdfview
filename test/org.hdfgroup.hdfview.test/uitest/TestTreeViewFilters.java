package uitest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;

import uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

public class TestTreeViewFilters extends AbstractWindowTest {
    private static final String testFilename = "tfilters.h5";
    String[][] filtersExpectedData = {
            { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" },
            { "10", "11", "12", "13", "14", "15", "16", "17", "18", "19" },
            { "20", "21", "22", "23", "24", "25", "26", "27", "28", "29" },
            { "30", "31", "32", "33", "34", "35", "36", "37", "38", "39" },
            { "40", "41", "42", "43", "44", "45", "46", "47", "48", "49" },
            { "50", "51", "52", "53", "54", "55", "56", "57", "58", "59" },
            { "60", "61", "62", "63", "64", "65", "66", "67", "68", "69" },
            { "70", "71", "72", "73", "74", "75", "76", "77", "78", "79" },
            { "80", "81", "82", "83", "84", "85", "86", "87", "88", "89" },
            { "90", "91", "92", "93", "94", "95", "96", "97", "98", "99" },
            { "100", "101", "102", "103", "104", "105", "106", "107", "108", "109" },
            { "110", "111", "112", "113", "114", "115", "116", "117", "118", "119" },
            { "120", "121", "122", "123", "124", "125", "126", "127", "128", "129" },
            { "130", "131", "132", "133", "134", "135", "136", "137", "138", "139" },
            { "140", "141", "142", "143", "144", "145", "146", "147", "148", "149" },
            { "150", "151", "152", "153", "154", "155", "156", "157", "158", "159" },
            { "160", "161", "162", "163", "164", "165", "166", "167", "168", "169" },
            { "170", "171", "172", "173", "174", "175", "176", "177", "178", "179" },
            { "180", "181", "182", "183", "184", "185", "186", "187", "188", "189" },
            { "190", "191", "192", "193", "194", "195", "196", "197", "198", "199" } };
    @Test
    public void openHDF5Filters() {
        SWTBotShell tableShell = null;
        File hdfFile = openFile(testFilename, FILE_MODE.READ_ONLY);

        try {
            TableDataRetriever retriever = null;
            SWTBotNatTable dataTable = null;

            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "openHDF5Filters()", 17, testFilename);

            /*
             * TODO: SZIP not available
             * tableShell = openTreeviewObject(filetree, testFilename, "all");
             * SWTBotNatTable dataTable = getNatTable(tableShell);
             *
             * TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);
             *
             * retriever.testAllTableLocations(filtersExpectedData);
             *
             * tableShell.bot().menu().menu("Table").menu("Close").click();
             * bot.waitUntil(Conditions.shellCloses(tableShell));
             */

            tableShell = openTreeviewObject(filetree, testFilename, "alloc_time_early");
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);

            retriever.testAllTableLocations(filtersExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            tableShell = openTreeviewObject(filetree, testFilename, "alloc_time_incr");
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);

            retriever.testAllTableLocations(filtersExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            tableShell = openTreeviewObject(filetree, testFilename, "alloc_time_late");
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);

            retriever.testAllTableLocations(filtersExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            tableShell = openTreeviewObject(filetree, testFilename, "chunked");
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);

            retriever.testAllTableLocations(filtersExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            tableShell = openTreeviewObject(filetree, testFilename, "compact");
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);

            retriever.testAllTableLocations(filtersExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            tableShell = openTreeviewObject(filetree, testFilename, "contiguous");
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);

            retriever.testAllTableLocations(filtersExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            tableShell = openTreeviewObject(filetree, testFilename, "deflate");
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);

            retriever.testAllTableLocations(filtersExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            tableShell = openTreeviewObject(filetree, testFilename, "fletcher32");
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);

            retriever.testAllTableLocations(filtersExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            /*
             * TODO: verify data in file
             * tableShell = openTreeviewObject(filetree, testFilename, "nbit"); dataTable
             * = getNatTable(tableShell);
             *
             * retriever = DataRetrieverFactory.getTableDataRetriever(dataTable,
             * "openHDF5Filters()", false);
             *
             * retriever.testAllTableLocations(filtersExpectedData);
             *
             * tableShell.bot().menu().menu("Table").menu("Close").click(); bot.waitUntil(Conditions.shellCloses(tableShell));
             */

            /*
             * TODO: verify data in file
             * tableShell = openTreeviewObject(filetree, testFilename, "scaleoffset"); dataTable
             * = getNatTable(tableShell);
             *
             * retriever = DataRetrieverFactory.getTableDataRetriever(dataTable,
             * "openHDF5Filters()", false);
             *
             * retriever.testAllTableLocations(filtersExpectedData);
             *
             * tableShell.bot().menu().menu("Table").menu("Close").click(); bot.waitUntil(Conditions.shellCloses(tableShell));
             */

            tableShell = openTreeviewObject(filetree, testFilename, "shuffle");
            dataTable = getNatTable(tableShell);

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);

            retriever.testAllTableLocations(filtersExpectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            /*
             * TODO: SZIP not available
             * tableShell = openTreeviewObject(filetree, testFilename, "szip");
             * dataTable = getNatTable(tableShell);
             *
             * retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openHDF5Filters()", false);
             *
             * retriever.testAllTableLocations(filtersExpectedData);
             *
             * tableShell.bot().menu().menu("Table").menu("Close").click();
             * bot.waitUntil(Conditions.shellCloses(tableShell));
             */
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu().menu("Table").menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {}
        }
    }

    @Test
    public void checkHDF5Filters() {
        String filtername = "fletcher32";
        File hdfFile = openFile(testFilename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "checkHDF5Filters()", 17, testFilename);

            SWTBotTabItem tabItem = openMetadataTab(filetree, testFilename, "all", "General Object Info");
            tabItem.activate();

            String val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "SHUFFLE: Nbytes = 4, SZIP, GZIP, Error detection filter, NBIT", val),
                    val.equals("SHUFFLE: Nbytes = 4, SZIP, GZIP, Error detection filter, NBIT"));

            tabItem = openMetadataTab(filetree, testFilename, "alloc_time_early", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "NONE", val),
                    val.equals("NONE"));

            tabItem = openMetadataTab(filetree, testFilename, "alloc_time_incr", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "NONE", val),
                    val.equals("NONE"));

            tabItem = openMetadataTab(filetree, testFilename, "alloc_time_late", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "NONE", val),
                    val.equals("NONE"));

            tabItem = openMetadataTab(filetree, testFilename, "chunked", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "NONE", val),
                    val.equals("NONE"));

            tabItem = openMetadataTab(filetree, testFilename, "compact", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "COMPACT", val),
                    val.equals("COMPACT"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "NONE", val),
                    val.equals("NONE"));

            tabItem = openMetadataTab(filetree, testFilename, "contiguous", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CONTIGUOUS", val),
                    val.equals("CONTIGUOUS"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "NONE", val),
                    val.equals("NONE"));

            tabItem = openMetadataTab(filetree, testFilename, "deflate", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "GZIP", val),
                    val.equals("GZIP"));

            tabItem = openMetadataTab(filetree, testFilename, "external", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            /*
             * TODO: fix regex
             * val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
             * assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data",
             * "CONTIGUOUS - EXTERNAL ", val), val.equals("CONTIGUOUS \\- EXTERNAL "));
             */            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "NONE", val),
                    val.equals("NONE"));

            tabItem = openMetadataTab(filetree, testFilename, "fletcher32", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "Error detection filter", val),
                    val.equals("Error detection filter"));

            tabItem = openMetadataTab(filetree, testFilename, "myfilter", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "USERDEFINED myfilter(405): 5, 6", val),
                    val.equals("USERDEFINED myfilter(405): 5, 6"));

            tabItem = openMetadataTab(filetree, testFilename, "nbit", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "NBIT", val),
                    val.equals("NBIT"));

            tabItem = openMetadataTab(filetree, testFilename, "scaleoffset", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "SCALEOFFSET: MIN BITS = 2", val),
                    val.equals("SCALEOFFSET: MIN BITS = 2"));

            tabItem = openMetadataTab(filetree, testFilename, "shuffle", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "SHUFFLE: Nbytes = 4", val),
                    val.equals("SHUFFLE: Nbytes = 4"));

            tabItem = openMetadataTab(filetree, testFilename, "szip", "General Object Info");
            tabItem.activate();

            val = bot.labelInGroup("Miscellaneous Dataset Information", 0).getText();
            assertTrue("label matches", val.equals("Storage Layout: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 1).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "CHUNKED: 10 X 5", val),
                    val.equals("CHUNKED: 10 X 5"));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 4).getText();
            assertTrue("label matches", val.equals("Filters: "));
            val = bot.labelInGroup("Miscellaneous Dataset Information", 5).getText();
            assertTrue(constructWrongValueMessage("checkHDF5Filters()", "wrong data", "SZIP", val),
                    val.equals("SZIP"));
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
            catch (Exception ex) {}
        }
    }
}
