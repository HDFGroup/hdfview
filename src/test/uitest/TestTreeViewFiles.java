package test.uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Ignore;
import org.junit.Test;

public class TestTreeViewFiles extends AbstractWindowTest {
    @Test
    public void openHDF5ScalarGroup() {
        String filename = "tscalarintsize.h5";
        String dataset_name = "DS08BITS";
        String dataset_name2 = "DU64BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5ScalarGroup()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==10);
            assertTrue("openHDF5ScalarGroup() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openHDF5ScalarGroup() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(dataset_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openHDF5ScalarGroup() data did not match regex '^[-1, .*]'",
                    tableShell.bot().text(0).getText().matches("^\\[-1, .*\\]"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(7).click();
            items[0].getNode(7).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(dataset_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openHDF5ScalarGroup() data did not match regex '^18446744073709551615, .*'",
                    tableShell.bot().text(0).getText().matches("^\\[18446744073709551615, .*\\]"));
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

    @Test
    public void openHDF5ScalarAttribute() {
        String filename = "tscalarattrintsize.h5";
        String[] attribute_names = {"DS08BITS", "DS16BITS", "DS32BITS", "DS64BITS", "DU08BITS", "DU16BITS", "DU32BITS", "DU64BITS"};
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5ScalarAttribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("openHDF5ScalarAttribute() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);

            items[0].click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            SWTBotTable attrTable = bot.table();

            for (int i = 0; i < attribute_names.length; i++) {
                String val = attrTable.cell(i, 0);
                assertTrue(constructWrongValueMessage("openHDF5ScalarAttribute()", "wrong attribute name", attribute_names[i], val),
                        val.equals(attribute_names[i]));
            }

            assertTrue("openHDF5ScalarAttribute() data did not match regex '-1, .*'", attrTable.cell(0, 3).matches("-1, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '-1, .*'", attrTable.cell(1, 3).matches("-1, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '-1, .*'", attrTable.cell(2, 3).matches("-1, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '-1, .*'", attrTable.cell(3, 3).matches("-1, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '255, .*'", attrTable.cell(4, 3).matches("255, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '65535, .*'", attrTable.cell(5, 3).matches("65535, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '4294967295, .*'", attrTable.cell(6, 3).matches("4294967295, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '18446744073709551615, .*'", attrTable.cell(7, 3).matches("18446744073709551615, .*"));
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
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openHDF5ScalarString() {
        String filename = "tscalarstring.h5";
        String datasetname = "the_str";
        String attr_name = "attr_str";
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5ScalarString()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("openHDF5ScalarString() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openHDF5ScalarString() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            SWTBotShell tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            String val = tableShell.bot().text(0).getText();

            String expected = "ABCDEFGHBCDEFGHICDEFGHIJDEFGHIJKEFGHIJKLFGHIJKLMGHIJKLMNHIJKLMNO";
            assertTrue(constructWrongValueMessage("openHDF5ScalarString()", "wrong data", expected, val), val.equals(expected));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            SWTBotTable attrTable = bot.table();

            val = attrTable.cell(0, 0);
            assertTrue(constructWrongValueMessage("openHDF5ScalarString()", "wrong attribute name", attr_name, val), val.equals(attr_name));

            val = attrTable.cell(0, 3);
            expected = "ABCDEFGHBCDEFGHICDEFGHIJDEFGHIJKEFGHIJKLFGHIJKLMGH";
            assertTrue(constructWrongValueMessage("openHDF5ScalarString()", "wrong attribute value", expected, val), val.equals(expected));
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
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openCreateOrderHDF5Group() {
        String filename = "tordergr.h5";
        String group1 = "1";
        String group2 = "2";
        String testgroup = "c";
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openCreateOrderHDF5Group()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==3);
            assertTrue("openCreateOrderHDF5Group() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openCreateOrderHDF5Group() filetree is missing group '" + group1 + "'", items[0].getNode(0).getText().compareTo(group1)==0);
            assertTrue("openCreateOrderHDF5Group() filetree is missing group '" + group2 + "'", items[0].getNode(1).getText().compareTo(group2)==0);

            items[0].click();
            items[0].contextMenu("Change file indexing").click();

            SWTBotShell indexingShell = bot.shell("Indexing options");
            indexingShell.activate();
            bot.waitUntil(Conditions.shellIsActive(indexingShell.getText()));

            indexingShell.bot().radioInGroup("By Creation Order", "Indexing Type").click();

            indexingShell.bot().button("   &Reload File   ").click();
            bot.waitUntil(Conditions.shellCloses(indexingShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openCreateOrderHDF5Group()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==3);
            assertTrue("openCreateOrderHDF5Group() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openCreateOrderHDF5Group() filetree is missing group '" + group2 + "'", items[0].getNode(0).getText().compareTo(group2)==0);
            assertTrue("openCreateOrderHDF5Group() filetree is missing group '" + group1 + "'", items[0].getNode(1).getText().compareTo(group1)==0);

            items[0].click();
            items[0].contextMenu("Change file indexing").click();

            indexingShell = bot.shell("Indexing options");
            indexingShell.activate();
            bot.waitUntil(Conditions.shellIsActive(indexingShell.getText()));

            indexingShell.bot().radioInGroup("Decrements", "Indexing Order").click();

            indexingShell.bot().button("   &Reload File   ").click();
            bot.waitUntil(Conditions.shellCloses(indexingShell));

            items = filetree.getAllItems();

            filetree.expandNode(items[0].getText(), true);

            assertTrue(constructWrongValueMessage("openCreateOrderHDF5Group()", "filetree wrong row count", "17", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==17);
            assertTrue("openCreateOrderHDF5Group() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openCreateOrderHDF5Group() filetree is missing group '" + group1 + "'", items[0].getNode(0).getText().compareTo(group1)==0);
            assertTrue("openCreateOrderHDF5Group() filetree is missing group '" + group2 + "'", items[0].getNode(1).getText().compareTo(group2)==0);
            assertTrue("openCreateOrderHDF5Group() filetree is missing group '" + testgroup + "'", items[0].getNode(0).getNode(2).getText().compareTo(testgroup)==0);
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
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openHDF5Attribute() {
        String filename = "tattrintsize.h5";
        String[] attrNames = {"DS08BITS", "DS16BITS", "DS32BITS", "DS64BITS", "DU08BITS", "DU16BITS", "DU32BITS", "DU64BITS"};
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5Attribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("openHDF5Attribute() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);

            items[0].click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            SWTBotTable attrTable = bot.table();

            for (int i = 0; i < attrNames.length; i++) {
                String val = attrTable.cell(i, 0);
                assertTrue(constructWrongValueMessage("openHDF5Attribute()", "wrong attribute name", attrNames[i], val),
                        val.equals(attrNames[i]));
            }

            assertTrue("openHDF5Attribute() data did not match regex '^-1, .*'", attrTable.cell(0, 3).matches("^-1, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^-1, .*'", attrTable.cell(1, 3).matches("^-1, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^-1, .*'", attrTable.cell(2, 3).matches("^-1, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^-1, .*'", attrTable.cell(3, 3).matches("^-1, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^255, .*'", attrTable.cell(4, 3).matches("^255, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^65535, .*'", attrTable.cell(5, 3).matches("^65535, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^4294967295, .*'", attrTable.cell(6, 3).matches("^4294967295, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^18446744073709551615, .*'", attrTable.cell(7, 3).matches("^18446744073709551615, .*"));
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
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openHDF5IntsAttribute() {
        String filename = "tintsattrs.h5";
        String datasetname = "DU64BITS";
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5IntsAttribute()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("openHDF5IntsAttribute() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openHDF5IntsAttribute() filetree is missing dataset '" + datasetname + "'", items[0].getNode(7).getText().compareTo(datasetname)==0);

            items[0].getNode(7).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            SWTBotTable attrTable = bot.table();

            String val = attrTable.cell(0, 0);
            assertTrue(constructWrongValueMessage("openHDF5IntsAttribute()", "wrong attribute name", datasetname, val),
                    val.equals(datasetname));

            // double-check attribute and open dialog for edit??
            // Beginning of Data
            assertTrue("openHDF5IntsAttribute() data did not match regex '^18446744073709551615, .*'", attrTable.cell(0, 3).matches("^18446744073709551615, .*"));
            // End of data
            // TODO disabled until non-visible scrolling available
            //assertTrue("openHDF5IntsAttribute() data did not match regex '^.*808, 0, 0, 0, 0, 0, 0, 0$'", attrTable.cell(0, 3).matches("^.*808, 0, 0, 0, 0, 0, 0, 0$"));
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
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openHDF5CompoundDS() {
        String filename = "tcmpdintsize.h5";
        String datasetname = "CompoundIntSize";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundDS()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("openHDF5CompoundDS() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openHDF5CompoundDS() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 5);
            assertTrue("openHDF5CompoundDS() data did not match regex '^[-1, .*]'",
                    tableShell.bot().text(0).getText().matches("^\\[-1, .*\\]"));

            table.click(4, 6);
            assertTrue("openHDF5CompoundDS() data did not match regex '^[-1, .*]'",
                    tableShell.bot().text(0).getText().matches("^\\[-1, .*\\]"));

            table.click(5, 7);
            assertTrue("openHDF5CompoundDS() data did not match regex '^[-1, .*]'",
                    tableShell.bot().text(0).getText().matches("^\\[-1, .*\\]"));

            // TODO Disabled until offscreen columns/rows can be accessed
            /*
             * table.click(6, 8);
             * assertTrue("openHDF5CompoundDS() data did not match regex '^-1, .*'"
             * , tableShell.bot().text(0).getText().matches("^-1, .*"));
             */

            table.click(3, 1);
            assertTrue("openHDF5CompoundDS() data did not match regex '^[255, .*]'",
                    tableShell.bot().text(0).getText().matches("^\\[255, .*\\]"));

            table.click(4, 2);
            assertTrue("openHDF5CompoundDS() data did not match regex '^[65535, .*]'",
                    tableShell.bot().text(0).getText().matches("^\\[65535, .*\\]"));

            table.click(5, 3);
            assertTrue("openHDF5CompoundDS() data did not match regex '^[4294967295, .*]'",
                    tableShell.bot().text(0).getText().matches("^\\[4294967295, .*\\]"));

            table.click(6, 4);
            assertTrue("openHDF5CompoundDS() data did not match regex '^[18446744073709551615, .*]'",
                    tableShell.bot().text(0).getText().matches("^\\[18446744073709551615, .*\\]"));
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

    @Test
    public void openHDF5CompoundDSints() {
        String filename = "tcmpdints.h5";
        String filename2 = "testintsfile2.h5";
        String datasetname1 = "CompoundInts";
        String datasetname2 = "CompoundRInts";
        SWTBotShell tableShell = null;
        openFile(filename, FILE_MODE.READ_ONLY);
        File hdf_save_file = new File(workDir, filename2);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==3);
            assertTrue("openHDF5CompoundDSints() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname1 + "'", items[0].getNode(0).getText().compareTo(datasetname1)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname2 + "'", items[0].getNode(1).getText().compareTo(datasetname2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname1 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            String val = table.getCellDataValueByPosition(3, 5);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-1", val),
                    val.equals("-1"));

            val = table.getCellDataValueByPosition(4, 6);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-2", val),
                    val.equals("-2"));

            val = table.getCellDataValueByPosition(5, 7);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-4", val),
                    val.equals("-4"));

            // TODO: SWTBot cannot retrieve values of cells that are not visible
            // val = table.getCellDataValueByPosition(6, 8);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-8", val),
            // val.equals("-8"));

            val = table.getCellDataValueByPosition(3, 1);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "255", val),
                    val.equals("255"));

            val = table.getCellDataValueByPosition(4, 2);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "65534", val),
                    val.equals("65534"));

            val = table.getCellDataValueByPosition(5, 3);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "4294967292", val),
                    val.equals("4294967292"));

            val = table.getCellDataValueByPosition(6, 4);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "18446744073709551608", val),
                    val.equals("18446744073709551608"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(1).click();
            items[0].getNode(1).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetname2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            //TODO: SWTBot cannot retrieve values of cells that are not visible
            // val = table.getCellDataValueByPosition(30, 8);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-8", val),
            // val.equals("-8"));
            //
            // val = table.getCellDataValueByPosition(29, 7);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-1024", val),
            // val.equals("-1024"));
            //
            // val = table.getCellDataValueByPosition(28, 6);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-33554432",
            // val),
            // val.equals("-33554432"));
            //
            // val = table.getCellDataValueByPosition(27, 5);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-16777216",
            // val),
            // val.equals("-16777216"));
            //
            // val = table.getCellDataValueByPosition(26, 4);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "128", val),
            // val.equals("128"));
            //
            // val = table.getCellDataValueByPosition(25, 3);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "65472", val),
            // val.equals("65472"));
            //
            // val = table.getCellDataValueByPosition(24, 2);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "4292870144",
            // val),
            // val.equals("4292870144"));
            //
            // val = table.getCellDataValueByPosition(23, 1);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data",
            // "18446744073708503040", val),
            // val.equals("18446744073708503040"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            if (hdf_save_file.exists()) hdf_save_file.delete();

            items[0].click();
            bot.menu("File").menu("Save As").click();

            SWTBotShell saveShell = bot.shell("Enter a file name");
            saveShell.activate();
            bot.waitUntil(Conditions.shellIsActive(saveShell.getText()));

            saveShell.bot().text().setText(filename2);

            val = saveShell.bot().text().getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong file name", filename2, val),
                    val.equals(filename2));

            saveShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(saveShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "filetree wrong row count", "6", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==6);
            assertTrue("openHDF5CompoundDSints() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname1 + "'", items[0].getNode(0).getText().compareTo(datasetname1)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname2 + "'", items[0].getNode(1).getText().compareTo(datasetname2)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing file '" + filename2 + "'", items[1].getText().compareTo(filename2)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname1 + "'", items[1].getNode(0).getText().compareTo(datasetname1)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname2 + "'", items[1].getNode(1).getText().compareTo(datasetname2)==0);

            items[1].getNode(1).click();
            items[1].getNode(1).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetname2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            //TODO: SWTBot cannot retrieve values of cells that are not visible
            // val = table.getCellDataValueByPosition(30, 8);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-8", val),
            // val.equals("-8"));
            //
            // val = table.getCellDataValueByPosition(29, 7);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-1024", val),
            // val.equals("-1024"));
            //
            // val = table.getCellDataValueByPosition(28, 6);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-33554432",
            // val),
            // val.equals("-33554432"));
            //
            // val = table.getCellDataValueByPosition(27, 5);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-16777216",
            // val),
            // val.equals("-16777216"));
            //
            // val = table.getCellDataValueByPosition(26, 4);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "128", val),
            // val.equals("128"));
            //
            // val = table.getCellDataValueByPosition(25, 3);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "65472", val),
            // val.equals("65472"));
            //
            // val = table.getCellDataValueByPosition(24, 2);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "4292870144",
            // val),
            // val.equals("4292870144"));
            //
            // val = table.getCellDataValueByPosition(23, 1);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data",
            // "18446744073708503040", val),
            // val.equals("18446744073708503040"));

            final SWTBotNatTable edittable = table;
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    edittable.doubleclick(3, 2);
                    edittable.widget.getActiveCellEditor().setEditorValue("0");
                    edittable.widget.getActiveCellEditor().commit(SelectionLayer.MoveDirectionEnum.RIGHT, true, true);
                }
            });

            val = table.getCellDataValueByPosition(3, 2);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "0", val),
                    val.equals("0"));

            tableShell.bot().menu("Table").menu("Save Changes to File").click();
            tableShell.bot().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[1].click();
            items[1].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[1].getText(), true);

            items[1].getNode(1).click();
            items[1].getNode(1).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetname2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            val = table.getCellDataValueByPosition(3, 2);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "0", val),
                    val.equals("0"));

            tableShell.bot().menu("Table").menu("Close").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_save_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openHDF5CompoundAttribute() {
        String filename = "tcmpdattrintsize.h5";
        String attr_name = "CompoundAttrIntSize";
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundAttribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("openHDF5CompoundAttribute() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);

            items[0].click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            SWTBotTable attrTable = bot.table();

            String val = attrTable.cell(0, 0);
            assertTrue(constructWrongValueMessage("openHDF5CompoundAttribute()", "wrong attribute name", attr_name, val),
                    val.equals(attr_name));

            assertTrue("openHDF5CompoundAttribute() data did not match regex '^.*[ 255.*].*'", attrTable.cell(0, 3).matches("^.*[ 255.*].*"));
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
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Ignore
    // TODO: disabled until import from template functionality is fixed
    public void openHDF5CompoundArrayImport() {
        String filename = "tcmpdintsize.h5";
        String filename2 = "temp_cmpimport.h5";
        String datasetname = "CompoundIntSize";
        String newDatasetName = "testcmpdname";
        String[] memberNames = {"DU08BITS", "DU16BITS", "DU32BITS", "DU64BITS", "DS08BITS", "DS16BITS", "DS32BITS", "DS64BITS"};
        SWTBotShell tableShell = null;

        try {
            File source = new File(workDir, filename);
            File dest = new File(workDir, filename2);

            CopyOption[] options = new CopyOption[] {
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES
            };

            Files.copy(source.toPath(), dest.toPath(), options);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        File hdf_file = openFile(filename2, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundArrayImport()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("openHDF5CompoundArrayImport() filetree is missing file '" + filename2 + "'", items[0].getText().compareTo(filename2)==0);
            assertTrue("openHDF5CompoundArrayImport() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname)==0);

            items[0].click();
            items[0].contextMenu("Compound DS").click();

            SWTBotShell newDatasetShell = bot.shell("New Compound Dataset...");
            newDatasetShell.activate();
            bot.waitUntil(Conditions.shellIsActive(newDatasetShell.getText()));

            newDatasetShell.bot().text(0).setText(newDatasetName);

            String val = newDatasetShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundArrayImport()", "wrong dataset name", newDatasetName, val),
                    val.equals(newDatasetName));

            newDatasetShell.bot().comboBox(1).setSelection(0);

            val = newDatasetShell.bot().comboBox(1).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundArrayImport()", "wrong template name", datasetname, val),
                    val.equals(datasetname));

            val = newDatasetShell.bot().comboBox(4).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundArrayImport()", "wrong number of members", "9", val),
                    val.equals("9"));

            newDatasetShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newDatasetShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundArrayImport()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==3);
            assertTrue("openHDF5CompoundArrayImport() filetree is missing file '" + filename2 + "'", items[0].getText().compareTo(filename2)==0);
            assertTrue("openHDF5CompoundArrayImport() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname)==0);
            assertTrue("openHDF5CompoundArrayImport() filetree is missing dataset '" + newDatasetName + "'", items[0].getNode(1).getText().compareTo(newDatasetName)==0);

            items[0].getNode(1).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex("Properties.*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            SWTBotShell metaDataShell = bot.shells()[1];
            metaDataShell.activate();
            bot.waitUntil(Conditions.shellIsActive(metaDataShell.getText()));

            SWTBotTable memberTable = metaDataShell.bot().table();

            for (int i = 0; i < memberNames.length; i++) {
                val = memberTable.cell(i, 0);
                assertTrue(constructWrongValueMessage("openHDF5CompoundArrayImport()", "wrong member name", memberNames[i], val),
                        val.equals(memberNames[i]));
            }

            metaDataShell.bot().button("   &Close   ").click();
            bot.waitUntil(Conditions.shellCloses(metaDataShell));

            items[0].getNode(1).click();
            items[0].getNode(1).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(newDatasetName + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            assertTrue(constructWrongValueMessage("openHDF5CompoundArrayImport()", "wrong column count", "9", String.valueOf(table.columnCount())),
                    table.columnCount() == 9);

            for (int i = 0; i < memberNames.length; i++) {
                val = table.getCellDataValueByPosition(0, (i + 1));
                assertTrue(constructWrongValueMessage("openHDF5CompoundArrayImport()", "Dataset column name mismatch", memberNames[i], val),
                        val.equals(memberNames[i]));
            }

            tableShell.bot().menu("Table").menu("Close").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openHDF5CompoundBits() {
        String filename = "tbitnopaque.h5";
        String groupname1 = "bittypetests";
        String groupname2 = "cmpdtypetests";
        String groupname3 = "opaquetypetests";
        String datasetname1 = "bitfield_1";
        String datasetname2 = "bitfield_2";
        String datasetname3 = "bitfield_3";
        String datasetname4 = "bitfield_4";
        String datasetname5 = "opaque_1";
        String datasetname6 = "opaque_2";
        String datasetname7 = "compound_1";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "filetree wrong row count", "4",
                    String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==4);
            assertTrue("openHDF5CompoundBits() filetree is missing file '" + filename + "'",
                    items[0].getText().compareTo(filename) == 0);
            assertTrue("openHDF5CompoundBits() filetree is missing group '" + groupname1 + "'",
                    items[0].getNode(0).getText().compareTo(groupname1) == 0);
            assertTrue("openHDF5CompoundBits() filetree is missing group '" + groupname2 + "'",
                    items[0].getNode(1).getText().compareTo(groupname2) == 0);
            assertTrue("openHDF5CompoundBits() filetree is missing group '" + groupname3 + "'",
                    items[0].getNode(2).getText().compareTo(groupname3) == 0);

            filetree.expandNode(filename, true);
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "filetree wrong row count", "11",
                    String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==11);

            items[0].getNode(0).getNode(0).click();
            items[0].getNode(0).getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname1 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FF", val), val.equals("FF"));

            table.click(2, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FE", val), val.equals("FE"));

            table.click(3, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FD", val), val.equals("FD"));

            table.click(4, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FC", val), val.equals("FC"));

            // TODO: disabled until a solution for getting values of non-visible cells is found
            //            val = table.getCellDataValueByPosition(31, 1);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundBits()",
            // "wrong data", "E1", val),
            //                    val.equals("E1"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(0).getNode(1).click();
            items[0].getNode(0).getNode(1).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetname2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FF:FF", val), val.equals("FF:FF"));

            table.click(2, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "EF:FF", val), val.equals("EF:FF"));

            table.click(3, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "DF:FF", val), val.equals("DF:FF"));

            table.click(4, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "CF:FF", val), val.equals("CF:FF"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(0).getNode(2).click();
            items[0].getNode(0).getNode(2).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetname3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FF:FF:FF:FF", val), val.equals("FF:FF:FF:FF"));

            table.click(2, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "DF:FF:FF:FF", val), val.equals("DF:FF:FF:FF"));

            table.click(3, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "BF:FF:FF:FF", val), val.equals("BF:FF:FF:FF"));

            table.click(4, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "9F:FF:FF:FF", val), val.equals("9F:FF:FF:FF"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(0).getNode(3).click();
            items[0].getNode(0).getNode(3).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetname4 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FF:FF:FF:FF:FF:FF:FF:FF", val), val.equals("FF:FF:FF:FF:FF:FF:FF:FF"));

            table.click(2, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "BF:FF:FF:FF:FF:FF:FF:FF", val), val.equals("BF:FF:FF:FF:FF:FF:FF:FF"));

            table.click(3, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "7F:FF:FF:FF:FF:FF:FF:FF", val), val.equals("7F:FF:FF:FF:FF:FF:FF:FF"));

            table.click(4, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "3F:FF:FF:FF:FF:FF:FF:FF", val), val.equals("3F:FF:FF:FF:FF:FF:FF:FF"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(0).click();
            items[0].getNode(2).getNode(0).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetname5 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FF", val), val.equals("FF"));

            table.click(2, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FE", val), val.equals("FE"));

            table.click(3, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FD", val), val.equals("FD"));

            table.click(4, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FC", val), val.equals("FC"));

            // TODO: disabled until a solution for getting values of non-visible cells is found
            // val = table.getCellDataValueByPosition(31, 1);
            // assertTrue(constructWrongValueMessage("openHDF5CompoundBits()",
            // "wrong data", "E1", val),
            // val.equals("E1"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(1).click();
            items[0].getNode(2).getNode(1).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetname6 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FF FF", val), val.equals("FF FF"));

            table.click(2, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "EF FF", val), val.equals("EF FF"));

            table.click(3, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "DF FF", val), val.equals("DF FF"));

            table.click(4, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "CF FF", val), val.equals("CF FF"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(1).getNode(0).click();
            items[0].getNode(1).getNode(0).contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetname7 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "FF", val), val.equals("FF"));

            table.click(4, 2);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "EF:FF", val), val.equals("EF:FF"));

            table.click(5, 3);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "BF:FF:FF:FF", val), val.equals("BF:FF:FF:FF"));

            table.click(6, 4);
            val = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundBits()", "wrong data", "3F:FF:FF:FF:FF:FF:FF:FF", val), val.equals("3F:FF:FF:FF:FF:FF:FF:FF"));

            tableShell.bot().menu("Close").click();
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

    @Test
    public void openHDF5ArrayString() {
        String filename = "tstr.h5";
        String datasetname = "comp1";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5ArrayString()", "filetree wrong row count", "6", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==6);
            assertTrue("openHDF5ArrayString() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openHDF5ArrayString() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue("openHDF5ArrayString() data did not match regex '^[0, 1, 4, .*]'", val.matches("^\\[0, 1, 4, .*\\]"));

            table.click(3, 2);
            val = tableShell.bot().text(0).getText();
            assertTrue("openHDF5ArrayString() data did not match regex '^[abcdefgh12345678abcdefgh12345678, abcdefgh12345678abcdefgh12345678, .*]'",
                    val.matches("^\\[abcdefgh12345678abcdefgh12345678, abcdefgh12345678abcdefgh12345678, .*\\]"));

            tableShell.bot().menu("Close").click();
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

    @Test
    public void openHDF5ArrayCompound() {
        String filename = "tarray4.h5";
        String datasetname = "Dataset1";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            String val;
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5ArrayCompound()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 2);
            assertTrue("openHDF5ArrayCompound() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
            assertTrue("openHDF5ArrayCompound() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname) == 0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue("openHDF5ArrayCompound() data did not match regex '0'", val.matches("0"));

            table.click(3, 2);
            val = tableShell.bot().text(0).getText();
            assertTrue("openHDF5ArrayCompound() data did not match regex '0.0'", val.matches("0.0"));

            table.click(4, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue("openHDF5ArrayCompound() data did not match regex '10'", val.matches("10"));

            table.click(4, 2);
            val = tableShell.bot().text(0).getText();
            assertTrue("openHDF5ArrayCompound() data did not match regex '2.5'", val.matches("2.5"));

            table.click(5, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue("openHDF5ArrayCompound() data did not match regex '20'", val.matches("20"));

            table.click(5, 2);
            val = tableShell.bot().text(0).getText();
            assertTrue("openHDF5ArrayCompound() data did not match regex '5.0'", val.matches("5.0"));

            table.click(6, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue("openHDF5ArrayCompound() data did not match regex '30'", val.matches("30"));

            table.click(6, 2);
            val = tableShell.bot().text(0).getText();
            assertTrue("openHDF5ArrayCompound() data did not match regex '7.5'", val.matches("7.5"));

            tableShell.bot().menu("Close").click();
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
            if (tableShell != null && tableShell.isOpen()) {
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
