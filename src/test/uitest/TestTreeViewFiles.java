package test.uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Ignore;
import org.junit.Test;

public class TestTreeViewFiles extends AbstractWindowTest {
    @Test
    public void openHDF5ScalarGroup() {
        String filename = "tscalarintsize";
        String file_ext = ".h5";
        String dataset_name = "DS08BITS";
        String dataset_name2 = "DU64BITS";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5ScalarGroup()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==10);
            assertTrue("openHDF5ScalarGroup() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openHDF5ScalarGroup() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(dataset_name + ".*at.*\\[.*in.*\\]")));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            assertTrue("openHDF5ScalarGroup() data did not match regex '^-1, .*'", table.getCellDataValueByPosition(1, 1).matches("^-1, .*"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(7).click();
            items[0].getNode(7).contextMenu().menu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(dataset_name2 + ".*at.*\\[.*in.*\\]")));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            assertTrue("openHDF5ScalarGroup() data did not match regex '^18446744073709551615, .*'", table.getCellDataValueByPosition(1, 1).matches("^18446744073709551615, .*"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        String filename = "tscalarattrintsize";
        String file_ext = ".h5";
        String[] attribute_names = {"DS08BITS", "DS16BITS", "DS32BITS", "DS64BITS", "DU08BITS", "DU16BITS", "DU32BITS", "DU64BITS"};
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5ScalarAttribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("openHDF5ScalarAttribute() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);

            items[0].click();
            items[0].contextMenu("Show Properties").click();

            SWTBotShell metaDataShell = bot.shell("Properties - /");
            metaDataShell.activate();
            bot.waitUntil(Conditions.shellIsActive(metaDataShell.getText()));

            metaDataShell.bot().tabItem("Attributes").activate();

            SWTBotTable attrTable = metaDataShell.bot().table();

            for (int i = 0; i < attribute_names.length; i++) {
                String val = attrTable.cell(i, 0);
                assertTrue(constructWrongValueMessage("openHDF5ScalarAttribute()", "wrong attribute name", attribute_names[i], val),
                        val.equals(attribute_names[i]));
            }

            assertTrue("openHDF5ScalarAttribute() data did not match regex '^. -1, .*'", attrTable.cell(0, 1).matches("^. -1, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '^. -1, .*'", attrTable.cell(1, 1).matches("^. -1, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '^. -1, .*'", attrTable.cell(2, 1).matches("^. -1, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '^. -1, .*'", attrTable.cell(3, 1).matches("^. -1, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '^. 255, .*'", attrTable.cell(4, 1).matches("^. 255, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '^. 65535, .*'", attrTable.cell(5, 1).matches("^. 65535, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '^. 4294967295, .*'", attrTable.cell(6, 1).matches("^. 4294967295, .*"));
            assertTrue("openHDF5ScalarAttribute() data did not match regex '^. 18446744073709551615, .*'", attrTable.cell(7, 1).matches("^. 18446744073709551615, .*"));

            metaDataShell.bot().button("   &Close   ").click();
            bot.waitUntil(Conditions.shellCloses(metaDataShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        String filename = "tscalarstring";
        String file_ext = ".h5";
        String datasetname = "the_str";
        String attr_name = "attr_str";
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5ScalarString()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("openHDF5ScalarString() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openHDF5ScalarString() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex("TextView.*" + datasetname + ".*" + filename + file_ext)));

            SWTBotShell textShell = bot.shells()[1];
            textShell.activate();
            bot.waitUntil(Conditions.shellIsActive(textShell.getText()));

            SWTBotTable textTable = textShell.bot().table();

            String val = textTable.cell(0, 1);
            String expected = "ABCDEFGHBCDEFGHICDEFGHIJDEFGHIJKEFGHIJKLFGHIJKLMGHIJKLMNHIJKLMNO";
            assertTrue(constructWrongValueMessage("openHDF5ScalarString()", "wrong data", expected, val), val.equals(expected));

            textShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(textShell));

            items[0].click();
            items[0].contextMenu("Show Properties").click();

            SWTBotShell metaDataShell = bot.shell("Properties - /");
            metaDataShell.activate();
            bot.waitUntil(Conditions.shellIsActive(metaDataShell.getText()));

            metaDataShell.bot().tabItem("Attributes").activate();

            SWTBotTable attrTable = metaDataShell.bot().table();

            val = attrTable.cell(0, 0);
            assertTrue(constructWrongValueMessage("openHDF5ScalarString()", "wrong attribute name", attr_name, val), val.equals(attr_name));

            val = attrTable.cell(0, 1);
            expected = "ABCDEFGHBCDEFGHICDEFGHIJDEFGHIJKEFGHIJKLFGHIJKLMGHIJKLMNHIJKLMNO";
            assertTrue(constructWrongValueMessage("openHDF5ScalarString()", "wrong attribute value", expected, val), val.equals(expected));

            metaDataShell.bot().button("   &Close   ").click();
            bot.waitUntil(Conditions.shellCloses(metaDataShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        String filename = "tordergr";
        String file_ext = ".h5";
        String group1 = "1";
        String group2 = "2";
        String testgroup = "c";
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openCreateOrderHDF5Group()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==3);
            assertTrue("openCreateOrderHDF5Group() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
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
            assertTrue("openCreateOrderHDF5Group() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
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
            assertTrue("openCreateOrderHDF5Group() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openCreateOrderHDF5Group() filetree is missing group '" + group1 + "'", items[0].getNode(0).getText().compareTo(group1)==0);
            assertTrue("openCreateOrderHDF5Group() filetree is missing group '" + group2 + "'", items[0].getNode(1).getText().compareTo(group2)==0);
            assertTrue("openCreateOrderHDF5Group() filetree is missing group '" + testgroup + "'", items[0].getNode(0).getNode(2).getText().compareTo(testgroup)==0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        String filename = "tattrintsize";
        String file_ext = ".h5";
        String[] attrNames = {"DS08BITS", "DS16BITS", "DS32BITS", "DS64BITS", "DU08BITS", "DU16BITS", "DU32BITS", "DU64BITS"};
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5Attribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("openHDF5Attribute() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);

            items[0].click();
            items[0].contextMenu("Show Properties").click();

            SWTBotShell metaDataShell = bot.shell("Properties - /");
            metaDataShell.activate();
            bot.waitUntil(Conditions.shellIsActive(metaDataShell.getText()));

            metaDataShell.bot().tabItem("Attributes").activate();

            SWTBotTable attrTable = metaDataShell.bot().table();

            for (int i = 0; i < attrNames.length; i++) {
                String val = attrTable.cell(i, 0);
                assertTrue(constructWrongValueMessage("openHDF5Attribute()", "wrong attribute name", attrNames[i], val),
                        val.equals(attrNames[i]));
            }

            assertTrue("openHDF5Attribute() data did not match regex '^-1, .*'", attrTable.cell(0, 1).matches("^-1, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^-1, .*'", attrTable.cell(1, 1).matches("^-1, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^-1, .*'", attrTable.cell(2, 1).matches("^-1, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^-1, .*'", attrTable.cell(3, 1).matches("^-1, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^255, .*'", attrTable.cell(4, 1).matches("^255, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^65535, .*'", attrTable.cell(5, 1).matches("^65535, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^4294967295, .*'", attrTable.cell(6, 1).matches("^4294967295, .*"));
            assertTrue("openHDF5Attribute() data did not match regex '^18446744073709551615, .*'", attrTable.cell(7, 1).matches("^18446744073709551615, .*"));

            metaDataShell.bot().button("   &Close   ").click();
            bot.waitUntil(Conditions.shellCloses(metaDataShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        String filename = "tintsattrs";
        String file_ext = ".h5";
        String datasetname = "DU64BITS";
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5IntsAttribute()", "filetree wrong row count", "10", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==10);
            assertTrue("openHDF5IntsAttribute() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openHDF5IntsAttribute() filetree is missing dataset '" + datasetname + "'", items[0].getNode(7).getText().compareTo(datasetname)==0);

            items[0].getNode(7).click();
            items[0].getNode(7).contextMenu("Show Properties").click();

            SWTBotShell metaDataShell = bot.shell("Properties - /" + datasetname);
            metaDataShell.activate();
            bot.waitUntil(Conditions.shellIsActive(metaDataShell.getText()));

            metaDataShell.bot().tabItem("Attributes").activate();

            SWTBotTable attrTable = metaDataShell.bot().table();

            String val = attrTable.cell(0, 0);
            assertTrue(constructWrongValueMessage("openHDF5IntsAttribute()", "wrong attribute name", datasetname, val),
                    val.equals(datasetname));

            assertTrue("openHDF5IntsAttribute() data did not match regex '^18446744073709551615, .*'", attrTable.cell(0, 1).matches("^18446744073709551615, .*"));
            assertTrue("openHDF5IntsAttribute() data did not match regex '^.*808, 0, 0, 0, 0, 0, 0, 0$'", attrTable.cell(0, 1).matches("^.*808, 0, 0, 0, 0, 0, 0, 0$"));

            metaDataShell.bot().button("   &Close   ").click();
            bot.waitUntil(Conditions.shellCloses(metaDataShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        String filename = "tcmpdintsize";
        String file_ext = ".h5";
        String datasetname = "CompoundIntSize";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundDS()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("openHDF5CompoundDS() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openHDF5CompoundDS() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]")));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            assertTrue("openHDF5CompoundDS() data did not match regex '^-1, .*'", table.getCellDataValueByPosition(3, 5).matches("^-1, .*"));
            assertTrue("openHDF5CompoundDS() data did not match regex '^-1, .*'", table.getCellDataValueByPosition(4, 6).matches("^-1, .*"));
            assertTrue("openHDF5CompoundDS() data did not match regex '^-1, .*'", table.getCellDataValueByPosition(5, 7).matches("^-1, .*"));
            assertTrue("openHDF5CompoundDS() data did not match regex '^-1, .*'", table.getCellDataValueByPosition(6, 8).matches("^-1, .*"));
            assertTrue("openHDF5CompoundDS() data did not match regex '^255, .*'", table.getCellDataValueByPosition(3, 1).matches("^255, .*"));
            assertTrue("openHDF5CompoundDS() data did not match regex '^65535, .*'", table.getCellDataValueByPosition(4, 2).matches("^65535, .*"));
            assertTrue("openHDF5CompoundDS() data did not match regex '^4294967295, .*'", table.getCellDataValueByPosition(5, 3).matches("^4294967295, .*"));
            assertTrue("openHDF5CompoundDS() data did not match regex '^18446744073709551615, .*'", table.getCellDataValueByPosition(6, 4).matches("^18446744073709551615, .*"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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

    @Ignore
    // TODO: disabled until a good solution for retrieving the value from non-visible cells is found
    public void openHDF5CompoundDSints() {
        String filename = "tcmpdints";
        String filename2 = "testintsfile2";
        String file_ext = ".h5";
        String datasetname1 = "CompoundInts";
        String datasetname2 = "CompoundRInts";
        SWTBotShell tableShell = null;
        openFile(filename, file_ext.equals(".h5") ? false : true);
        File hdf_save_file = new File(workDir, filename2 + file_ext);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==3);
            assertTrue("openHDF5CompoundDSints() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname1 + "'", items[0].getNode(0).getText().compareTo(datasetname1)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname2 + "'", items[0].getNode(1).getText().compareTo(datasetname2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(datasetname1 + ".*at.*\\[.*in.*\\]")));

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

            val = table.getCellDataValueByPosition(6, 8);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-8", val),
                    val.equals("-8"));

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
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(datasetname2 + ".*at.*\\[.*in.*\\]")));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            //TODO: SWTBot cannot retrieve values of cells that are not visible
            val = table.getCellDataValueByPosition(30, 8);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-8", val),
                    val.equals("-8"));

            val = table.getCellDataValueByPosition(29, 7);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-1024", val),
                    val.equals("-1024"));

            val = table.getCellDataValueByPosition(28, 6);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-33554432", val),
                    val.equals("-33554432"));

            val = table.getCellDataValueByPosition(27, 5);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-16777216", val),
                    val.equals("-16777216"));

            val = table.getCellDataValueByPosition(26, 4);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "128", val),
                    val.equals("128"));

            val = table.getCellDataValueByPosition(25, 3);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "65472", val),
                    val.equals("65472"));

            val = table.getCellDataValueByPosition(24, 2);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "4292870144", val),
                    val.equals("4292870144"));

            val = table.getCellDataValueByPosition(23, 1);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "18446744073708503040", val),
                    val.equals("18446744073708503040"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            if (hdf_save_file.exists()) hdf_save_file.delete();

            items[0].click();
            bot.menu("File").menu("Save As").click();

            SWTBotShell saveShell = bot.shell("Enter a file name");
            saveShell.activate();
            bot.waitUntil(Conditions.shellIsActive(saveShell.getText()));

            saveShell.bot().text().setText(filename2 + file_ext);

            val = saveShell.bot().text().getText();
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong file name", filename2 + file_ext, val),
                    val.equals(filename2 + file_ext));

            saveShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(saveShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "filetree wrong row count", "6", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==6);
            assertTrue("openHDF5CompoundDSints() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname1 + "'", items[0].getNode(0).getText().compareTo(datasetname1)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname2 + "'", items[0].getNode(1).getText().compareTo(datasetname2)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing file '" + filename2 + file_ext + "'", items[1].getText().compareTo(filename2 + file_ext)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname1 + "'", items[1].getNode(0).getText().compareTo(datasetname1)==0);
            assertTrue("openHDF5CompoundDSints() filetree is missing dataset '" + datasetname2 + "'", items[1].getNode(1).getText().compareTo(datasetname2)==0);

            items[1].getNode(1).click();
            items[1].getNode(1).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(datasetname2 + ".*at.*\\[.*in.*\\]")));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            //TODO: SWTBot cannot retrieve values of cells that are not visible
            val = table.getCellDataValueByPosition(30, 8);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-8", val),
                    val.equals("-8"));

            val = table.getCellDataValueByPosition(29, 7);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-1024", val),
                    val.equals("-1024"));

            val = table.getCellDataValueByPosition(28, 6);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-33554432", val),
                    val.equals("-33554432"));

            val = table.getCellDataValueByPosition(27, 5);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "-16777216", val),
                    val.equals("-16777216"));

            val = table.getCellDataValueByPosition(26, 4);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "128", val),
                    val.equals("128"));

            val = table.getCellDataValueByPosition(25, 3);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "65472", val),
                    val.equals("65472"));

            val = table.getCellDataValueByPosition(24, 2);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "4292870144", val),
                    val.equals("4292870144"));

            val = table.getCellDataValueByPosition(23, 1);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "18446744073708503040", val),
                    val.equals("18446744073708503040"));

            table.doubleclick(1, 2);
            bot.waitUntil(Conditions.waitForWidget(WidgetOfType.widgetOfType(org.eclipse.swt.widgets.Text.class)));
            tableShell.bot().text().setText("0");

            // Press enter to set value
            table.pressShortcut(SWT.NONE, SWT.CR, ' ');

            val = table.getCellDataValueByPosition(1, 2);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "0", val),
                    val.equals("0"));

            tableShell.bot().menu("Table").menu("Save Changes to File").click();
            tableShell.bot().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[1].click();
            items[1].contextMenu("Reload File").click();

            items[1].getNode(1).click();
            items[1].getNode(1).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(datasetname2 + ".*at.*\\[.*in.*\\]")));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            val = table.getCellDataValueByPosition(1, 2);
            assertTrue(constructWrongValueMessage("openHDF5CompoundDSints()", "wrong data", "0", val),
                    val.equals("0"));

            tableShell.bot().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
//            try {
//                closeFile(hdf_file, false);
//            }
//            catch (Exception ex) {
//                ex.printStackTrace();
//            }
        }
    }

    @Test
    public void openHDF5CompoundAttribute() {
        String filename = "tcmpdattrintsize";
        String file_ext = ".h5";
        String attr_name = "CompoundAttrIntSize";
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundAttribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("openHDF5CompoundAttribute() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);

            items[0].click();
            items[0].contextMenu("Show Properties").click();

            SWTBotShell metaDataShell = bot.shell("Properties - /");
            metaDataShell.activate();
            bot.waitUntil(Conditions.shellIsActive(metaDataShell.getText()));

            metaDataShell.bot().tabItem("Attributes").activate();

            SWTBotTable attrTable = metaDataShell.bot().table();

            String val = attrTable.cell(0, 0);
            assertTrue(constructWrongValueMessage("openHDF5CompoundAttribute()", "wrong attribute name", attr_name, val),
                    val.equals(attr_name));

            assertTrue("openHDF5CompoundAttribute() data did not match regex '^.*[ 255.*].*'", attrTable.cell(0, 1).matches("^.*[ 255.*].*"));

            metaDataShell.bot().button("   &Close   ").click();
            bot.waitUntil(Conditions.shellCloses(metaDataShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        String filename = "tcmpdintsize";
        String filename2 = "temp_cmpimport";
        String file_ext = ".h5";
        String datasetname = "CompoundIntSize";
        String newDatasetName = "testcmpdname";
        String[] memberNames = {"DU08BITS", "DU16BITS", "DU32BITS", "DU64BITS", "DS08BITS", "DS16BITS", "DS32BITS", "DS64BITS"};
        SWTBotShell tableShell = null;

        try {
            File source = new File(workDir, filename + file_ext);
            File dest = new File(workDir, filename2 + file_ext);

            CopyOption[] options = new CopyOption[] {
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            };

            Files.copy(source.toPath(), dest.toPath(), options);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        File hdf_file = openFile(filename2, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openHDF5CompoundArrayImport()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("openHDF5CompoundArrayImport() filetree is missing file '" + filename2 + file_ext + "'", items[0].getText().compareTo(filename2 + file_ext)==0);
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
            assertTrue("openHDF5CompoundArrayImport() filetree is missing file '" + filename2 + file_ext + "'", items[0].getText().compareTo(filename2 + file_ext)==0);
            assertTrue("openHDF5CompoundArrayImport() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getText().compareTo(datasetname)==0);
            assertTrue("openHDF5CompoundArrayImport() filetree is missing dataset '" + newDatasetName + "'", items[0].getNode(1).getText().compareTo(newDatasetName)==0);

            items[0].getNode(1).click();
            items[0].getNode(1).contextMenu("Show Properties").click();

            SWTBotShell metaDataShell = bot.shell("Properties - /" + newDatasetName);
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
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(newDatasetName + ".*at.*\\[.*in.*\\]")));

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
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
}
