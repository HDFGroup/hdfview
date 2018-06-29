package test.uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

public class TestHDFViewTAttr2 extends AbstractWindowTest {
    @Test
    public void openTAttr2GroupArray() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name = "array";
        String datasetg2_name2 = "array2D";
        String datasetg2_name3 = "array3D";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupArray()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupArray() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupArray() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupArray() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupArray() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(0).click();
            items[0].getNode(2).getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openTAttr2GroupArray() data did not match regex '1, 2, 3'",
                    tableShell.bot().text(0).getText().matches("1, 2, 3"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(1).click();
            items[0].getNode(2).getNode(1).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(2, 2);
            assertTrue("openTAttr2GroupArray() data did not match regex '10, 11, 12'",
                    tableShell.bot().text(0).getText().matches("10, 11, 12"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(2).click();
            items[0].getNode(2).getNode(2).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 3);
            assertTrue("openTAttr2GroupArray() data ["+tableShell.bot().text(2).getText()+"] did not match regex '49, 50, 51'",
                    tableShell.bot().text(2).getText().matches("49, 50, 51"));
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
    public void openTAttr2GroupBitfield() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name = "bitfield";
        String datasetg2_name2 = "bitfield2D";
        String datasetg2_name3 = "bitfield3D";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupBitfield()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupBitfield() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupBitfield() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupBitfield() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupBitfield() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(3).click();
            items[0].getNode(2).getNode(3).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openTAttr2GroupBitfield() data did not match regex '01'",
                    tableShell.bot().text(0).getText().matches("01"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(4).click();
            items[0].getNode(2).getNode(4).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(2, 2);
            assertTrue("openTAttr2GroupBitfield() data did not match regex '04'",
                    tableShell.bot().text(0).getText().matches("04"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(5).click();
            items[0].getNode(2).getNode(5).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 3);
            assertTrue("openTAttr2GroupBitfield() data ["+tableShell.bot().text(2).getText()+"] did not match regex '11'",
                    tableShell.bot().text(2).getText().matches("11"));
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
    public void openTAttr2GroupCompound() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name = "compound";
        String datasetg2_name2 = "compound2D";
        String datasetg2_name3 = "compound3D";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupCompound()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupCompound() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupCompound() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupCompound() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupCompound() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(6).click();
            items[0].getNode(2).getNode(6).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 1);
            assertTrue("openTAttr2GroupCompound() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    tableShell.bot().text(0).getText()+"] did not match regex '1'",
                    tableShell.bot().text(0).getText().matches("1"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(7).click();
            items[0].getNode(2).getNode(7).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(4, 2);
            assertTrue("openTAttr2GroupCompound() data ["+tableShell.bot().text(0).getText()+"] did not match regex '6.0'",
                    tableShell.bot().text(0).getText().matches("6.0"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(8).click();
            items[0].getNode(2).getNode(8).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(5, 3);
            assertTrue("openTAttr2GroupCompound() data ["+tableShell.bot().text(2).getText()+"] did not match regex '29'",
                    tableShell.bot().text(2).getText().matches("29"));
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
    public void openTAttr2GroupEnum() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name = "enum";
        String datasetg2_name2 = "enum2D";
        String datasetg2_name3 = "enum3D";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            // switch to ViewProperties Convert Enum
            SWTBotMenu fileMenuItem = bot.menu("Tools").menu("User Options");
            fileMenuItem.click();

            SWTBotShell botshell = bot.shell("Preferences");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Preferences"));

            botshell.bot().tree().getTreeItem("HDF Settings").click();

            SWTBotCheckBox enumButton = botshell.bot().checkBox("Convert Enum");
            if (!enumButton.isChecked())
                enumButton.click();

            botshell.bot().button("Apply and Close").click();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupEnum()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupEnum() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupEnum() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupEnum() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupEnum() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(9).click();
            items[0].getNode(2).getNode(9).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openTAttr2GroupEnum() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    tableShell.bot().text(0).getText()+"] did not match regex 'RED'",
                    tableShell.bot().text(0).getText().matches("RED"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(10).click();
            items[0].getNode(2).getNode(10).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(2, 2);
            assertTrue("openTAttr2GroupEnum() data ["+tableShell.bot().text(0).getText()+"] did not match regex 'RED'",
                    tableShell.bot().text(0).getText().matches("RED"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(11).click();
            items[0].getNode(2).getNode(11).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 3);
            assertTrue("openTAttr2GroupEnum() data ["+tableShell.bot().text(2).getText()+"] did not match regex 'RED'",
                    tableShell.bot().text(2).getText().matches("RED"));
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
    public void openTAttr2GroupFloat() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name = "float";
        String datasetg2_name2 = "float2D";
        String datasetg2_name3 = "float3D";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupFloat()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupFloat() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupFloat() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupFloat() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupFloat() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(12).click();
            items[0].getNode(2).getNode(12).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openTAttr2GroupFloat() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    tableShell.bot().text(0).getText()+"] did not match regex '1.0'",
                    tableShell.bot().text(0).getText().matches("1.0"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(13).click();
            items[0].getNode(2).getNode(13).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(2, 2);
            assertTrue("openTAttr2GroupFloat() data ["+tableShell.bot().text(0).getText()+"] did not match regex '4.0'",
                    tableShell.bot().text(0).getText().matches("4.0"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(14).click();
            items[0].getNode(2).getNode(14).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 3);
            assertTrue("openTAttr2GroupFloat() data ["+tableShell.bot().text(2).getText()+"] did not match regex '17.0'",
                    tableShell.bot().text(2).getText().matches("17.0"));
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
    public void openTAttr2GroupInteger() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name = "integer";
        String datasetg2_name2 = "integer2D";
        String datasetg2_name3 = "integer3D";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupInteger()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupInteger() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupInteger() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupInteger() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupInteger() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(15).click();
            items[0].getNode(2).getNode(15).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openTAttr2GroupInteger() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    tableShell.bot().text(0).getText()+"] did not match regex '1'",
                    tableShell.bot().text(0).getText().matches("1"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(16).click();
            items[0].getNode(2).getNode(16).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(2, 2);
            assertTrue("openTAttr2GroupInteger() data ["+tableShell.bot().text(0).getText()+"] did not match regex '4'",
                    tableShell.bot().text(0).getText().matches("4"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(17).click();
            items[0].getNode(2).getNode(17).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 3);
            assertTrue("openTAttr2GroupInteger() data ["+tableShell.bot().text(2).getText()+"] did not match regex '17'",
                    tableShell.bot().text(2).getText().matches("17"));
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
    public void openTAttr2GroupOpaque() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name = "opaque";
        String datasetg2_name2 = "opaque2D";
        String datasetg2_name3 = "opaque3D";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupOpaque()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupOpaque() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupOpaque() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupOpaque() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupOpaque() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(18).click();
            items[0].getNode(2).getNode(18).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openTAttr2GroupOpaque() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    tableShell.bot().text(0).getText()+"] did not match regex '01'",
                    tableShell.bot().text(0).getText().matches("01"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(19).click();
            items[0].getNode(2).getNode(19).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(2, 2);
            assertTrue("openTAttr2GroupOpaque() data ["+tableShell.bot().text(0).getText()+"] did not match regex '04'",
                    tableShell.bot().text(0).getText().matches("04"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(20).click();
            items[0].getNode(2).getNode(20).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 3);
            assertTrue("openTAttr2GroupOpaque() data ["+tableShell.bot().text(2).getText()+"] did not match regex '11'",
                    tableShell.bot().text(2).getText().matches("11"));
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
    public void openTAttr2GroupReference() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name = "reference";
        String datasetg2_name2 = "reference2D";
        String datasetg2_name3 = "reference3D";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupReference()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupReference() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupReference() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupReference() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupReference() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(21).click();
            items[0].getNode(2).getNode(21).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openTAttr2GroupReference() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    tableShell.bot().text(0).getText()+"] did not match regex '/dset'",
                    tableShell.bot().text(0).getText().matches("/dset"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(22).click();
            items[0].getNode(2).getNode(22).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(2, 2);
            assertTrue("openTAttr2GroupReference() data ["+tableShell.bot().text(0).getText()+"] did not match regex '/dset'",
                    tableShell.bot().text(0).getText().matches("/dset"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(23).click();
            items[0].getNode(2).getNode(23).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 3);
            assertTrue("openTAttr2GroupReference() data ["+tableShell.bot().text(2).getText()+"] did not match regex '/dset'",
                    tableShell.bot().text(2).getText().matches("/dset"));
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
    public void openTAttr2GroupString() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name = "string";
        String datasetg2_name2 = "string2D";
        String datasetg2_name3 = "string3D";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupString()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupString() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupString() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupString() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupString() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(24).click();
            items[0].getNode(2).getNode(24).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openTAttr2GroupString() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    tableShell.bot().text(0).getText()+"] did not match regex 'ab'",
                    tableShell.bot().text(0).getText().matches("ab"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(25).click();
            items[0].getNode(2).getNode(25).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(2, 2);
            assertTrue("openTAttr2GroupString() data ["+
                    tableShell.bot().text(0).getText()+"] did not match regex 'gh'",
                    tableShell.bot().text(0).getText().matches("gh"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(26).click();
            items[0].getNode(2).getNode(26).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(4, 2);
            assertTrue("openTAttr2GroupString() data ["+
                    tableShell.bot().text(2).getText()+"] did not match regex 'RS'",
                    tableShell.bot().text(2).getText().matches("RS"));
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
    public void openTAttr2GroupVlen() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name = "vlen";
        String datasetg2_name2 = "vlen2D";
        String datasetg2_name3 = "vlen3D";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupVlen()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupVlen() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupVlen() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupVlen() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupVlen() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(27).click();
            items[0].getNode(2).getNode(27).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            assertTrue("openTAttr2GroupVlen() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    tableShell.bot().text(0).getText()+"] did not match regex '{1}'",
                    tableShell.bot().text(0).getText().equals("{1}"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(28).click();
            items[0].getNode(2).getNode(28).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name2 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(2, 2);
            assertTrue("openTAttr2GroupVlen() data ["+tableShell.bot().text(0).getText()+"] did not match regex '{4, 5}'",
                    tableShell.bot().text(0).getText().equals("{4, 5}"));

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(2).getNode(29).click();
            items[0].getNode(2).getNode(29).contextMenu().menu("Open").click();
            shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(3, 3);
            assertTrue("openTAttr2GroupVlen() data ["+tableShell.bot().text(2).getText()+"] did not match regex '{30, 31, 32}'",
                    tableShell.bot().text(2).getText().equals("{30, 31, 32}"));
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
    public void openTAttr2Attribute() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2Attribute()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2Attribute() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2Attribute() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2Attribute() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2Attribute() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            SWTBotTable table = new SWTBotTable(bot.widget(widgetOfType(Table.class)));

            table.click(0, 0);
            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(0,0)+"] did not match regex 'array'",
                    table.cell(0,0).matches("array"));

            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(0,3)+"] did not match regex '1, 2, 3, 4, 5, 6'",
                    table.cell(0,3).matches("1, 2, 3, 4, 5, 6"));

            table.click(1, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(1,0)+
                    "] did not match regex 'array2D'",
                    table.cell(1,0).matches("array2D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(1,3)+
                    "] did not match regex '1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18'",
                    table.cell(1,3).matches("1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18"));

            table.click(2, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(2,0)+
                    "] did not match regex 'array3D'",
                    table.cell(2,0).matches("array3D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(2,3)+
                    "] did not match regex '1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72'",
                    //table.cell(2,3).matches("1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72"));
                    table.cell(2,3).matches("1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50"));

            table.click(3, 0);
            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(3,0)+"] did not match regex 'bitfield'",
                    table.cell(3,0).matches("bitfield"));

            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(3,3)+"] did not match regex '1, 2'",
                    table.cell(3,3).matches("1, 2"));

            table.click(4, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(4,0)+
                    "] did not match regex 'bitfield2D'",
                    table.cell(4,0).matches("bitfield2D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(4,3)+
                    "] did not match regex '1, 2, 3, 4, 5, 6'",
                    table.cell(4,3).matches("1, 2, 3, 4, 5, 6"));

            table.click(5, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(5,0)+
                    "] did not match regex 'bitfield3D'",
                    table.cell(5,0).matches("bitfield3D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(5,3)+
                    "] did not match regex '1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24'",
                    table.cell(5,3).matches("1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24"));

            table.click(6, 0);
            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(6,0)+"] did not match regex 'compound'",
                    table.cell(6,0).matches("compound"));

            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(6,3)+"] did not match regex ' {1, 2} ,  {3, 4} '",
                    table.cell(6,3).matches(" \\{1, 2\\} ,  \\{3, 4\\} "));

            table.click(7, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(7,0)+
                    "] did not match regex 'compound2D'",
                    table.cell(7,0).matches("compound2D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(7,3)+
                    "] did not match regex ' {1, 2} ,  {3, 4} ,  {5, 6} ,  {7, 8} ,  {9, 10} ,  {11, 12} '",
                    table.cell(7,3).matches(" \\{1, 2\\} ,  \\{3, 4\\} ,  \\{5, 6\\} ,  \\{7, 8\\} ,  \\{9, 10\\} ,  \\{11, 12\\} "));

            table.click(8, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(8,0)+
                    "] did not match regex 'compound3D'",
                    table.cell(8,0).matches("compound3D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(8,3)+
                    "] did not match regex ' {1, 2} ,  {3, 4} ,  {5, 6} ,  {7, 8} ,  {9, 10} ,  {11, 12} ,  {13, 14} ,  {15, 16} ,  {17, 18} ,  {19, 20} ,  {21, 22} ,  {23, 24} ,  {25, 26} ,  {27, 28} ,  {29, 30} ,  {31, 32} ,  {33, 34} ,  {35, 36} ,  {37, 38} ,  {39, 40} ,  {41, 42} ,  {43, 44} ,  {45, 46} ,  {47, 48} '",
                    table.cell(8,3).matches(" \\{1, 2\\} ,  \\{3, 4\\} ,  \\{5, 6\\} ,  \\{7, 8\\} ,  \\{9, 10\\} ,  \\{11, 12\\} ,  \\{13, 14\\} ,  \\{15, 16\\} ,  \\{17, 18\\} ,  \\{19, 20\\} ,  \\{21, 22\\} ,  \\{23, 24\\} ,  \\{25, 26\\} ,  \\{27, 28\\} ,  \\{29, 30\\} ,  \\{31, 32\\} ,  \\{33, 34\\} ,  \\{35, 36\\} ,  \\{37, 38\\} ,  \\{39, 40\\} ,  \\{41, 42\\} ,  \\{43, 44\\} ,  \\{45, 46\\} ,  \\{47, 48\\} "));

            table.click(9, 0);
            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(9,0)+"] did not match regex 'enum'",
                    table.cell(9,0).matches("enum"));

            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(9,3)+"] did not match regex 'RED, RED'",
                    table.cell(9,3).matches("RED, RED"));

            table.click(10, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(10,0)+
                    "] did not match regex 'enum2D'",
                    table.cell(10,0).matches("enum2D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(10,3)+
                    "] did not match regex 'RED, RED, RED, RED, RED, RED'",
                    table.cell(10,3).matches("RED, RED, RED, RED, RED, RED"));

            table.click(11, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(11,0)+
                    "] did not match regex 'enum3D'",
                    table.cell(11,0).matches("enum3D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(11,3)+
                    "] did not match regex 'RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED'",
                    table.cell(11,3).matches("RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED, RED"));

            table.click(12, 0);
            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(12,0)+"] did not match regex 'float'",
                    table.cell(12,0).matches("float"));

            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(12,3)+"] did not match regex '1.0, 2.0'",
                    table.cell(12,3).matches("1.0, 2.0"));

            table.click(13, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(13,0)+
                    "] did not match regex 'float2D'",
                    table.cell(13,0).matches("float2D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(13,3)+
                    "] did not match regex '1.0, 2.0, 3.0, 4.0, 5.0, 6.0'",
                    table.cell(13,3).matches("1.0, 2.0, 3.0, 4.0, 5.0, 6.0"));

            table.click(14, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(14,0)+
                    "] did not match regex 'float3D'",
                    table.cell(14,0).matches("float3D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(14,3)+
                    "] did not match regex '1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0'",
                    table.cell(14,3).matches("1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0"));

            table.click(15, 0);
            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(15,0)+"] did not match regex 'integer'",
                    table.cell(15,0).matches("integer"));

            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(15,3)+"] did not match regex '1, 2'",
                    table.cell(15,3).matches("1, 2"));

            table.click(16, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(16,0)+
                    "] did not match regex 'integer2D'",
                    table.cell(16,0).matches("integer2D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(16,3)+
                    "] did not match regex '1, 2, 3, 4, 5, 6'",
                    table.cell(16,3).matches("1, 2, 3, 4, 5, 6"));

            table.click(17, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(17,0)+
                    "] did not match regex 'integer3D'",
                    table.cell(17,0).matches("integer3D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(17,3)+
                    "] did not match regex '1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24'",
                    table.cell(17,3).matches("1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24"));

            table.click(18, 0);
            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(18,0)+"] did not match regex 'opaque'",
                    table.cell(18,0).matches("opaque"));

            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(18,3)+"] did not match regex '1, 2'",
                    table.cell(18,3).matches("1, 2"));

            table.click(19, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(19,0)+
                    "] did not match regex 'opaque2D'",
                    table.cell(19,0).matches("opaque2D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(19,3)+
                    "] did not match regex '1, 2, 3, 4, 5, 6'",
                    table.cell(19,3).matches("1, 2, 3, 4, 5, 6"));

            table.click(20, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(20,0)+
                    "] did not match regex 'opaque3D'",
                    table.cell(20,0).matches("opaque3D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(20,3)+
                    "] did not match regex '1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24'",
                    table.cell(20,3).matches("1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24"));

            table.click(21, 0);
            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(21,0)+"] did not match regex 'reference'",
                    table.cell(21,0).matches("reference"));

            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(21,3)+"] did not match regex '976, 976'",
                    table.cell(21,3).matches("976, 976"));

            table.click(22, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(22,0)+
                    "] did not match regex 'reference2D'",
                    table.cell(22,0).matches("reference2D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(22,3)+
                    "] did not match regex '976, 976, 976, 976, 976, 976'",
                    table.cell(22,3).matches("976, 976, 976, 976, 976, 976"));

            table.click(23, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(23,0)+
                    "] did not match regex 'reference3D'",
                    table.cell(23,0).matches("reference3D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(23,3)+
                    "] did not match regex '976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976'",
                    table.cell(23,3).matches("976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976, 976"));

            table.click(24, 0);
            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(24,0)+"] did not match regex 'string'",
                    table.cell(24,0).matches("string"));

            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(24,3)+"] did not match regex 'ab, de'",
                    table.cell(24,3).matches("ab, de"));

            table.click(25, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(25,0)+
                    "] did not match regex 'string2D'",
                    table.cell(25,0).matches("string2D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(25,3)+
                    "] did not match regex 'ab, cd, ef, gh, ij, kl'",
                    table.cell(25,3).matches("ab, cd, ef, gh, ij, kl"));

            table.click(26, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(26,0)+
                    "] did not match regex 'string3D'",
                    table.cell(26,0).matches("string3D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(26,3)+
                    "] did not match regex 'ab, cd, ef, gh, ij, kl, mn, pq, rs, tu, vw, xz, AB, CD, EF, GH, IJ, KL, MN, PQ, RS, TU, VW, XZ'",
                    table.cell(26,3).matches("ab, cd, ef, gh, ij, kl, mn, pq, rs, tu, vw, xz, AB, CD, EF, GH, IJ, KL, MN, PQ, RS, TU, VW, XZ"));

            table.click(27, 0);
            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(27,0)+"] did not match regex 'vlen'",
                    table.cell(27,0).matches("vlen"));

            assertTrue("openTAttr2Attribute() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(27, 3) + "] did not match regex '{1}, {2, 3}'", table.cell(27, 3).matches("\\{1\\}, \\{2, 3\\}"));

            table.click(28, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(28,0)+
                    "] did not match regex 'vlen2D'",
                    table.cell(28,0).matches("vlen2D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(28,3)+
                    "] did not match regex '{0}, {1}, {2, 3}, {4, 5}, {6, 7, 8}, {9, 10, 11}'",
                    table.cell(28, 3).matches("\\{0\\}, \\{1\\}, \\{2, 3\\}, \\{4, 5\\}, \\{6, 7, 8\\}, \\{9, 10, 11\\}"));

            table.click(29, 0);
            assertTrue("openTAttr2Attribute() data ["+table.cell(29,0)+
                    "] did not match regex 'rvlen3D'",
                    table.cell(29,0).matches("vlen3D"));

            assertTrue("openTAttr2Attribute() data ["+table.cell(29,3)+
                    "] did not match regex '{0}, {1}, {2}, {3}, {4}, {5}, {6, 7}, {8, 9}, {10, 11}, {12, 13}, {14, 15}, {16, 17}, {18, 19, 20}, {21, 22, 23}, {24, 25, 26}, {27, 28, 29}, {30, 31, 32}, {33, 34, 35}, {36, 37, 38, 39}, {40, 41, 42, 43}, {44, 45, 46, 47}, {48, 49, 50, 51}, {52, 53, 54, 55}, {56, 57, 58, 59}'",
                    table.cell(29, 3).matches(
                            "\\{0\\}, \\{1\\}, \\{2\\}, \\{3\\}, \\{4\\}, \\{5\\}, \\{6, 7\\}, \\{8, 9\\}, \\{10, 11\\}, \\{12, 13\\}, \\{14, 15\\}, \\{16, 17\\}, \\{18, 19, 20\\}, \\{21, 22, 23\\}, \\{24, 25, 26\\}, \\{27, 28, 29\\}, \\{30, 31, 32\\}, \\{33, 34, 35\\}, \\{36, 37, 38, 39\\}, \\{40, 41, 42, 43\\}, \\{44, 45, 46, 47\\}, \\{48, 49, 50, 51\\}, \\{52, 53, 54, 55\\}, \\{56, 57, 58, 59\\}"));
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
    public void openTAttr2GroupReferenceAsTable() {
        String filename = "tattr2";
        String file_ext = ".h5";
        String dataset_name = "dset";
        String group_name = "g1";
        String group_name2 = "g2";
        String datasetg2_name3 = "reference3D";
        SWTBotShell tableShell = null;
        SWTBotShell table2Shell = null;
        File hdf_file = openFile(filename, file_ext.equals(".h5") ? false : true);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttr2GroupReferenceAsTable()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==4);
            assertTrue("openTAttr2GroupReferenceAsTable() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("openTAttr2GroupReferenceAsTable() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttr2GroupReferenceAsTable() filetree is missing group '" + group_name + "'", items[0].getNode(1).getText().compareTo(group_name)==0);
            assertTrue("openTAttr2GroupReferenceAsTable() filetree is missing group '" + group_name2 + "'", items[0].getNode(2).getText().compareTo(group_name2)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Expand All").click();

            items[0].getNode(2).getNode(23).click();
            items[0].getNode(2).getNode(23).contextMenu().menu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetg2_name3 + ".*at.*\\[.*in.*\\]");
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
