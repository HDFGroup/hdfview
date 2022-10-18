package uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withRegex;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.hamcrest.Matcher;
import org.junit.Test;

import uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

public class TestTreeViewExport extends AbstractWindowTest {
    private String filename = "testdsimp.h5";
    private String groupname = "testgroupname";

    private File createImportHDF5Dataset(String datasetname) {
        String datasetdimsize = "8 x 64";
        File hdfFile = createFile(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createImportHDF5Dataset()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createImportHDF5Dataset() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);

            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Group").click();

            SWTBotShell groupShell = bot.shell("New Group...");
            groupShell.activate();
            bot.waitUntil(Conditions.shellIsActive(groupShell.getText()));

            groupShell.bot().text(0).setText(groupname);

            String val = groupShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("createImportHDF5Dataset()", "wrong group name", groupname, val),
                    val.equals(groupname));

            groupShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(groupShell));

            assertTrue(constructWrongValueMessage("createImportHDF5Dataset()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("createImportHDF5Dataset() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("createImportHDF5Dataset() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname)==0);

            items[0].getNode(0).click();

            items[0].getNode(0).contextMenu().contextMenu("New").menu("Dataset").click();

            SWTBotShell datasetShell = bot.shell("New Dataset...");
            datasetShell.activate();
            bot.waitUntil(Conditions.shellIsActive(datasetShell.getText()));

            datasetShell.bot().text(0).setText(datasetname);
            datasetShell.bot().text(2).setText(datasetdimsize);

            // Create 64-bit dataset
            datasetShell.bot().comboBox(3).setSelection("64");

            val = datasetShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("createImportHDF5Dataset()", "wrong dataset name", datasetname, val),
                    val.equals(datasetname));

            val = datasetShell.bot().text(2).getText();
            assertTrue(constructWrongValueMessage("createImportHDF5Dataset()", "wrong dataset dimension sizes", datasetdimsize, val),
                    val.equals(datasetdimsize));

            datasetShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(datasetShell));

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Expand All").click();

            assertTrue(constructWrongValueMessage("createImportHDF5Dataset()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==3);
            assertTrue("createImportHDF5Dataset() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("createImportHDF5Dataset() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname)==0);
            assertTrue("createImportHDF5Dataset() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getNode(0).getText().compareTo(datasetname)==0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }

        return hdfFile;
    }

    private void importHDF5Dataset(File hdfFile, String datasetName, String importfilename, String[][] expectedData) {
        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "importHDF5Dataset()", 3, hdfFile.getName());

            SWTBotShell tableShell = openTreeviewObject(filetree, hdfFile.getName(),
                    "/" + groupname + "/" + datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "importHDF5Dataset()", false);

            dataTable.click(1, 1);
            tableShell.bot().menu().menu("Import/Export Data").menu("Import Data from").menu("Text File").click();

            SWTBotShell importShell = bot.shell("Enter a file name");
            importShell.activate();

            SWTBotText text = importShell.bot().text();
            text.setText(importfilename);

            String val = text.getText();
            assertTrue("importHDF5Dataset() wrong file name: expected '" + importfilename + "' but was '" + val + "'",
                    val.equals(importfilename));

            importShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(importShell));

            Matcher<Shell> classMatcher = widgetOfType(Shell.class);
            Matcher<Shell> regexMatcher = withRegex(".*Import.*");
            @SuppressWarnings("unchecked")
            Matcher<Shell> shellMatcher = allOf(classMatcher, regexMatcher);
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            final SWTBotShell botShell = new SWTBotShell(bot.widget(shellMatcher));

            botShell.activate();
            bot.waitUntil(Conditions.shellIsActive(botShell.getText()));

            botShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(botShell));

            retriever.testAllTableLocations(expectedData);

            tableShell.bot().menu().menu("Table").menu("Close").click();

            regexMatcher = withRegex("Changes Detected");
            shellMatcher = allOf(classMatcher, regexMatcher);
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            final SWTBotShell saveShell = new SWTBotShell(bot.widget(shellMatcher));

            saveShell.activate();
            bot.waitUntil(Conditions.shellIsActive(saveShell.getText()));

            saveShell.bot().button("Cancel").click();
            bot.waitUntil(Conditions.shellCloses(saveShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

    @Test
    public void saveHDF5DatasetText() {
        String fname = "tintsize.h5";
        String groupsetname = "DS64BITS";
        SWTBotShell exportShell = null;
        File hdfFile = openFile(fname, FILE_MODE.READ_WRITE);
        File export_file = null;

        try {
            new File(workDir, groupsetname + ".txt").delete();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "saveHDF5DatasetText()", 10, fname);

            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].getNode(3).click();
            items[0].getNode(3).contextMenu().contextMenu("Export Dataset").menu("Export Data to Text File").click();

            Matcher<Shell> classMatcher = widgetOfType(Shell.class);
            Matcher<Shell> regexMatcher = withRegex("Save Dataset Data To Text File.*");
            @SuppressWarnings("unchecked")
            Matcher<Shell> shellMatcher = allOf(classMatcher, regexMatcher);
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            exportShell = new SWTBotShell(bot.widget(shellMatcher));

            exportShell.activate();
            bot.waitUntil(Conditions.shellIsActive(exportShell.getText()));

            SWTBotText text = exportShell.bot().text();
            text.setText(groupsetname + ".txt");

            String val = text.getText();
            assertTrue("saveHDF5DatasetText() wrong file name: expected '" + groupsetname + ".txt" + "' but was '" + val + "'",
                    val.equals(groupsetname + ".txt"));

            exportShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(exportShell));

            export_file = new File(workDir, groupsetname + ".txt");
            assertTrue("File-export text file created", export_file.exists());
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
            closeShell(exportShell);

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void saveHDF5DatasetBinary() {
        String fname = "tintsize.h5";
        String groupsetname = "DU64BITS";
        SWTBotShell exportShell = null;
        File hdfFile = openFile(fname, FILE_MODE.READ_WRITE);
        File export_file = null;

        try {
            new File(workDir, groupsetname + ".bin").delete();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Expand All").click();

            assertTrue(constructWrongValueMessage("saveHDF5DatasetBinary()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("saveHDF5DatasetBinary() filetree is missing file '" + fname + "'", items[0].getText().compareTo(fname) == 0);
            assertTrue("saveHDF5DatasetBinary() filetree is missing group '" + groupsetname + "'", items[0].getNode(0).getText().compareTo("DS08BITS")==0);

            items[0].getNode(3).click();
            items[0].getNode(3).contextMenu().contextMenu("Export Dataset").menu("Export Data as Little Endian").click();

            Matcher<Shell> classMatcher = widgetOfType(Shell.class);
            Matcher<Shell> regexMatcher = withRegex("Save Current Data To Binary File.*");
            @SuppressWarnings("unchecked")
            Matcher<Shell> shellMatcher = allOf(classMatcher, regexMatcher);
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            exportShell = new SWTBotShell(bot.widget(shellMatcher));

            exportShell.activate();
            bot.waitUntil(Conditions.shellIsActive(exportShell.getText()));

            SWTBotText text = exportShell.bot().text();
            text.setText(groupsetname + ".bin");

            String val = text.getText();
            assertTrue("saveHDF5DatasetText() wrong file name: expected '" + groupsetname + ".bin" + "' but was '" + val + "'",
                    val.equals(groupsetname + ".bin"));

            exportShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(exportShell));

            export_file = new File(workDir, "DU64BITS.bin");
            assertTrue("File-export binary file created", export_file.exists());
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
            closeShell(exportShell);

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void importHDF5DatasetWithTab() {
        String[][] expectedData =
            { { "-1", "-2", "-4", "-8", "-16", "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768",
                "-65536", "-131072", "-262144", "-524288", "-1048576", "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648",
                "-4294967296", "-8589934592", "-17179869184", "-34359738368", "-68719476736", "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328",
                "-281474976710656", "-562949953421312", "-1125899906842624", "-2251799813685248", "-4503599627370496", "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808" },
              { "-2", "-4", "-8", "-16", "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536",
                "-131072", "-262144", "-524288", "-1048576", "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296",
                "-8589934592", "-17179869184", "-34359738368", "-68719476736", "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656",
                "-562949953421312", "-1125899906842624", "-2251799813685248", "-4503599627370496", "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0" },
              { "-4", "-8", "-16", "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072",
                "-262144", "-524288", "-1048576", "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592",
                "-17179869184", "-34359738368", "-68719476736", "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312",
                "-1125899906842624", "-2251799813685248", "-4503599627370496", "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0" },
              { "-8", "-16", "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072", "-262144",
                "-524288", "-1048576", "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592", "-17179869184",
                "-34359738368", "-68719476736", "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312", "-1125899906842624",
                "-2251799813685248", "-4503599627370496", "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0", "0" },
              { "-16", "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072", "-262144", "-524288",
                "-1048576", "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592", "-17179869184", "-34359738368",
                "-68719476736", "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312", "-1125899906842624", "-2251799813685248",
                "-4503599627370496", "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0", "0", "0" },
              { "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072", "-262144", "-524288", "-1048576",
                "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592", "-17179869184", "-34359738368", "-68719476736",
                "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312", "-1125899906842624", "-2251799813685248", "-4503599627370496",
                "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0", "0", "0", "0" },
              { "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072", "-262144", "-524288", "-1048576", "-2097152",
                "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592", "-17179869184", "-34359738368", "-68719476736", "-137438953472",
                "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312", "-1125899906842624", "-2251799813685248", "-4503599627370496", "-9007199254740992",
                "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0", "0", "0", "0", "0" },
              { "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072", "-262144", "-524288", "-1048576", "-2097152", "-4194304",
                "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592", "-17179869184", "-34359738368", "-68719476736", "-137438953472", "-274877906944",
                "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312", "-1125899906842624", "-2251799813685248", "-4503599627370496", "-9007199254740992", "-18014398509481984",
                "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0", "0", "0", "0", "0" } };
        String datasetName = "testdatasetab";
        File hdfFile = null;

        try {
            // switch to ViewProperties.DELIMITER_TAB
            SWTBotMenu fileMenuItem = bot.menu().menu("Tools");
            fileMenuItem.menu("User Options").click();

            SWTBotShell botshell = bot.shell("Preferences");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Preferences"));

            SWTBotCombo combo = botshell.bot().comboBox(4);
            combo.setSelection("Tab");

            botshell.bot().button("Apply and Close").click();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }

        try {
            SWTBotTree filetree = bot.tree();

            hdfFile = createImportHDF5Dataset(datasetName);
            importHDF5Dataset(hdfFile, datasetName, "DS64BITS.ttxt", expectedData);

            checkFileTree(filetree, "importHDF5DatasetWithTab()", 3, filename);
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
                closeFile(hdfFile, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void importHDF5DatasetWithComma() {
        String[][] expectedData =
            { { "-1", "-2", "-4", "-8", "-16", "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768",
                "-65536", "-131072", "-262144", "-524288", "-1048576", "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648",
                "-4294967296", "-8589934592", "-17179869184", "-34359738368", "-68719476736", "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328",
                "-281474976710656", "-562949953421312", "-1125899906842624", "-2251799813685248", "-4503599627370496", "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808" },
              { "-2", "-4", "-8", "-16", "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536",
                "-131072", "-262144", "-524288", "-1048576", "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296",
                "-8589934592", "-17179869184", "-34359738368", "-68719476736", "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656",
                "-562949953421312", "-1125899906842624", "-2251799813685248", "-4503599627370496", "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0" },
              { "-4", "-8", "-16", "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072",
                "-262144", "-524288", "-1048576", "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592",
                "-17179869184", "-34359738368", "-68719476736", "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312",
                "-1125899906842624", "-2251799813685248", "-4503599627370496", "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0" },
              { "-8", "-16", "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072", "-262144",
                "-524288", "-1048576", "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592", "-17179869184",
                "-34359738368", "-68719476736", "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312", "-1125899906842624",
                "-2251799813685248", "-4503599627370496", "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0", "0" },
              { "-16", "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072", "-262144", "-524288",
                "-1048576", "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592", "-17179869184", "-34359738368",
                "-68719476736", "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312", "-1125899906842624", "-2251799813685248",
                "-4503599627370496", "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0", "0", "0" },
              { "-32", "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072", "-262144", "-524288", "-1048576",
                "-2097152", "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592", "-17179869184", "-34359738368", "-68719476736",
                "-137438953472", "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312", "-1125899906842624", "-2251799813685248", "-4503599627370496",
                "-9007199254740992", "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0", "0", "0", "0" },
              { "-64", "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072", "-262144", "-524288", "-1048576", "-2097152",
                "-4194304", "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592", "-17179869184", "-34359738368", "-68719476736", "-137438953472",
                "-274877906944", "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312", "-1125899906842624", "-2251799813685248", "-4503599627370496", "-9007199254740992",
                "-18014398509481984", "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0", "0", "0", "0", "0" },
              { "-128", "-256", "-512", "-1024", "-2048", "-4096", "-8192", "-16384", "-32768", "-65536", "-131072", "-262144", "-524288", "-1048576", "-2097152", "-4194304",
                "-8388608", "-16777216", "-33554432", "-67108864", "-134217728", "-268435456", "-536870912", "-1073741824", "-2147483648", "-4294967296", "-8589934592", "-17179869184", "-34359738368", "-68719476736", "-137438953472", "-274877906944",
                "-549755813888", "-1099511627776", "-2199023255552", "-4398046511104", "-8796093022208", "-17592186044416", "-35184372088832", "-70368744177664", "-140737488355328", "-281474976710656", "-562949953421312", "-1125899906842624", "-2251799813685248", "-4503599627370496", "-9007199254740992", "-18014398509481984",
                "-36028797018963968", "-72057594037927936", "-144115188075855872", "-288230376151711744", "-576460752303423488", "-1152921504606846976", "-2305843009213693952", "-4611686018427387904", "-9223372036854775808", "0", "0", "0", "0", "0", "0" } };
        String datasetName = "testdatasetcomma";
        File hdfFile = null;

        try {
            //switch to ViewProperties.DELIMITER_COMMA
            SWTBotMenu fileMenuItem = bot.menu().menu("Tools");
            fileMenuItem.menu("User Options").click();

            SWTBotShell botshell = bot.shell("Preferences");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Preferences"));

            SWTBotCombo combo = botshell.bot().comboBox(4);
            combo.setSelection("Comma");

            botshell.bot().button("Apply and Close").click();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }

        try {
            SWTBotTree filetree = bot.tree();

            hdfFile = createImportHDF5Dataset(datasetName);
            importHDF5Dataset(hdfFile, datasetName, "DS64BITS.xtxt", expectedData);

            checkFileTree(filetree, "importHDF5DatasetWithComma()", 3, filename);
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
                closeFile(hdfFile, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
