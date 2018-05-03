package test.uitest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.LayerCommandUtil;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.nebula.widgets.nattable.viewport.command.ShowColumnInViewportCommand;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;

import static org.hamcrest.Matcher.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.Ignore;

public class TestHDFViewCutCopyPaste extends AbstractWindowTest {
    private String groupname = "test_group";
    private String datasetname = "test_dataset";

    private void createNewHDF5Group() {
        SWTBotShell groupShell = null;

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].click();
            items[0].contextMenu("New").menu("Group").click();

            groupShell = bot.shell("New Group...");
            groupShell.activate();
            bot.waitUntil(Conditions.shellIsActive(groupShell.getText()));

            groupShell.bot().text(0).setText(groupname);

            String val = groupShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("createNewHDF5Group()", "wrong group name", groupname, val), val.equals(groupname));

            groupShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(groupShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            if (groupShell != null && groupShell.isOpen()) {
                groupShell.close();
                bot.waitUntil(Conditions.shellCloses(groupShell));
            }
        }
    }

    private void createNewHDF5Dataset() {
        String currentSize = "4 x 4";
        SWTBotShell tableShell = null;
        SWTBotShell datasetShell = null;

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("New").menu("Dataset").click();

            datasetShell = bot.shell("New Dataset...");
            datasetShell.activate();
            bot.waitUntil(Conditions.shellIsActive(datasetShell.getText()));

            datasetShell.bot().text(0).setText(datasetname);

            String val = datasetShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "wrong dataset name", datasetname, val), val.equals(datasetname));

            datasetShell.bot().text(2).setText(currentSize);

            val = datasetShell.bot().text(2).getText();
            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "wrong current size", currentSize, val), val.equals(currentSize));

            datasetShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(datasetShell));

            filetree.expandNode(items[0].getText(), true);
            items = filetree.getAllItems();

            items[0].getNode(0).getNode(0).click();
            items[0].getNode(0).getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    for (int row = 1; row <= 4; row++) {
                        for (int col = 1; col <= 4; col++) {
                            String val = String.valueOf(((row - 1) * 4) + (col));
                            table.doubleclick(row, col);
                            table.widget.getActiveCellEditor().setEditorValue(val);
                            table.widget.getActiveCellEditor().commit(SelectionLayer.MoveDirectionEnum.RIGHT, true, true);
                            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "wrong value", val, table.getCellDataValueByPosition(row, col)),
                                    table.getCellDataValueByPosition(row, col).equals(val));
                        }
                    }
                }
            });

            tableShell.bot().menu("Table").menu("Save Changes to File").click();

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
            if (datasetShell != null && datasetShell.isOpen()) {
                datasetShell.close();
                bot.waitUntil(Conditions.shellCloses(datasetShell));
            }

            if (tableShell != null && tableShell.isOpen()) {
                tableShell.close();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }
        }
    }

    @Test
    public void testCopyPasteGroupInSameFile() {
        String filename = "testcopypaste";
        String file_ext = ".h5";
        String group_copy_name = "test_group~copy";
        String dataset_copy_name = "test_dataset_copy";
        SWTBotShell tableShell = null;

        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testCopyPasteGroupInSameFile()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("testCopyPasteGroupInSameFile() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext) == 0);

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(constructWrongValueMessage("testCopyPasteGroupInSameFile()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 3);
            assertTrue("testCopyPasteGroupInSameFile() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCopyPasteGroupInSameFile() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Copy").click();

            items[0].click();
            items[0].contextMenu("Paste").click();

            SWTBotShell copyTargetShell = bot.shell("Copy object");
            copyTargetShell.activate();
            copyTargetShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(copyTargetShell));

            // Reload file
            items[0].click();
            items[0].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(constructWrongValueMessage("testCopyPasteGroupInSameFile()", "filetree wrong row count", "5", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 5);
            assertTrue("testCopyPasteGroupInSameFile() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext) == 0);
            assertTrue("testCopyPasteGroupInSameFile() filetree is missing group '" + group_copy_name + "'",
                    items[0].getNode(1).getText().compareTo(group_copy_name) == 0);

            items[0].getNode(1).click();
            items[0].getNode(1).getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table2 = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            for (int row = 1; row <= table2.preferredRowCount() - 1; row++) {
                for (int col = 1; col < table2.preferredColumnCount(); col++) {
                    String expected = String.valueOf(((row - 1) * (table2.preferredColumnCount() - 1)) + (col));
                    String val = table2.getCellDataValueByPosition(row, col);
                    assertTrue(constructWrongValueMessage("testCopyPasteGroupInSameFile()", "wrong data", expected, val), val.equals(expected));
                }
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
            catch (Exception ex) {}
        }
    }

    @Test
    public void testCopyPasteGroupInDifferentFile() {
        String filename = "testcopy";
        String filenameTo = "testpaste";
        String file_ext = ".h5";
        SWTBotShell tableShell = null;

        File hdf_file = createHDF5File(filename);
        File hdf_file2 = createHDF5File(filenameTo);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(constructWrongValueMessage("testCopyPasteGroupInDifferentFile()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 4);
            assertTrue("testCopyPasteGroupInDifferentFile() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCopyPasteGroupInDifferentFile() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Copy").click();

            items[1].click();
            items[1].contextMenu("Paste").click();

            SWTBotShell copyTargetShell = bot.shell("Copy object");
            copyTargetShell.activate();
            copyTargetShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(copyTargetShell));

            // Reload file
            items[0].click();
            items[0].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            // Reload file
            items[1].click();
            items[1].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[1].getText(), true);

            assertTrue(constructWrongValueMessage("testCopyPasteGroupInDifferentFile()", "filetree wrong row count", "6", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 6);
            assertTrue("testCopyPasteGroupInDifferentFile() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCopyPasteGroupInDifferentFile() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);
            assertTrue("testCopyPasteGroupInDifferentFile() filetree is missing group '" + groupname + "'", items[1].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCopyPasteGroupInDifferentFile() filetree is missing dataset '" + datasetname + "'",
                    items[1].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[1].getNode(0).click();
            items[1].getNode(0).getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table2 = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            for (int row = 1; row <= table2.preferredRowCount() - 1; row++) {
                for (int col = 1; col < table2.preferredColumnCount(); col++) {
                    String expected = String.valueOf(((row - 1) * (table2.preferredColumnCount() - 1)) + (col));
                    String val = table2.getCellDataValueByPosition(row, col);
                    assertTrue(constructWrongValueMessage("testCopyPasteGroupInDifferentFile()", "wrong data", expected, val), val.equals(expected));
                }
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
            try {
                if (hdf_file2 != null)
                    closeFile(hdf_file2, true);
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }
}
