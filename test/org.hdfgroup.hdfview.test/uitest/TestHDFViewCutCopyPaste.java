package uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

public class TestHDFViewCutCopyPaste extends AbstractWindowTest {
    private String groupname = "test_group";
    private String datasetname = "test_dataset";

    private void createNewHDF5Group() {
        SWTBotShell groupShell = null;

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Group").click();

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
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
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
            items[0].getNode(0).contextMenu().contextMenu("New").menu("Dataset").click();

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
            items[0].getNode(0).getNode(0).contextMenu().contextMenu("Open").click();
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

            tableShell.bot().menu().menu("Table").menu("Save Changes to File").click();

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
        String filename = "testcopypaste.h5";
        String group_copy_name = "test_group~copy";
        String dataset_copy_name = "test_dataset_copy";
        SWTBotShell tableShell = null;
        File hdf_file = createFile(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testCopyPasteGroupInSameFile()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("testCopyPasteGroupInSameFile() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(constructWrongValueMessage("testCopyPasteGroupInSameFile()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 3);
            assertTrue("testCopyPasteGroupInSameFile() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCopyPasteGroupInSameFile() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Copy").click();

            items[0].click();
            items[0].contextMenu().contextMenu("Paste").click();

            SWTBotShell copyTargetShell = bot.shells()[1];
            copyTargetShell.activate();
            copyTargetShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(copyTargetShell));

            // Reload file
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(constructWrongValueMessage("testCopyPasteGroupInSameFile()", "filetree wrong row count", "5", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 5);
            assertTrue("testCopyPasteGroupInSameFile() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
            assertTrue("testCopyPasteGroupInSameFile() filetree is missing group '" + group_copy_name + "'",
                    items[0].getNode(1).getText().compareTo(group_copy_name) == 0);

            items[0].getNode(1).click();
            items[0].getNode(1).getNode(0).contextMenu().contextMenu("Open").click();
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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
        String filename = "testcopy.h5";
        String filenameTo = "testpaste.h5";
        SWTBotShell tableShell = null;

        File hdf_file = createFile(filename);
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            assertTrue(constructWrongValueMessage("testCopyPasteGroupInDifferentFile()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("testCopyPasteGroupInDifferentFile() HDF filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }

        File hdf_file2 = createFile(filenameTo);
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            assertTrue(constructWrongValueMessage("testCopyPasteGroupInDifferentFile()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 2);
            assertTrue("testCopyPasteGroupInDifferentFile() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
            assertTrue("testCopyPasteGroupInDifferentFile() filetree is missing file '" + filenameTo + "'", items[1].getText().compareTo(filenameTo) == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
            try {
                if (hdf_file.exists())
                    closeFile(hdf_file, true);
            }
            catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
            try {
                if (hdf_file.exists())
                    closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }

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
            items[0].getNode(0).contextMenu().contextMenu("Copy").click();

            items[1].click();
            items[1].contextMenu().contextMenu("Paste").click();

            SWTBotShell copyTargetShell = bot.shells()[1];
            copyTargetShell.activate();
            copyTargetShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(copyTargetShell));

            // Reload file
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            // Reload file
            items[1].click();
            items[1].contextMenu().contextMenu("Reload File").click();

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
            items[1].getNode(0).getNode(0).contextMenu().contextMenu("Open").click();
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
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.close();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file2, true);
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
            }
        }
    }

    @Test
    public void testCutPasteGroupInDifferentFile() {
        String filename = "testcopy.h5";
        String filenameTo = "testpaste.h5";
        SWTBotShell tableShell = null;

        File hdf_file = createFile(filename);
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            assertTrue(constructWrongValueMessage("testCutPasteGroupInDifferentFile()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("testCutPasteGroupInDifferentFile() HDF filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }

        File hdf_file2 = createFile(filenameTo);
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            assertTrue(constructWrongValueMessage("testCutPasteGroupInDifferentFile()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 2);
            assertTrue("testCutPasteGroupInDifferentFile() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
            assertTrue("testCutPasteGroupInDifferentFile() filetree is missing file '" + filenameTo + "'", items[1].getText().compareTo(filenameTo) == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
            try {
                if (hdf_file.exists())
                    closeFile(hdf_file, true);
            }
            catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
            try {
                if (hdf_file.exists())
                    closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(constructWrongValueMessage("testCutPasteGroupInDifferentFile()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 4);
            assertTrue("testCutPasteGroupInDifferentFile() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCutPasteGroupInDifferentFile() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Cut").click();

            items[1].click();
            items[1].contextMenu().contextMenu("Paste").click();

            SWTBotShell copyTargetShell = bot.shells()[1];
            copyTargetShell.activate();
            copyTargetShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(copyTargetShell));

            // Reload file
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            // Reload file
            items[1].click();
            items[1].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[1].getText(), true);

            assertTrue(constructWrongValueMessage("testCutPasteGroupInDifferentFile()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 4);
            assertTrue("testCutPasteGroupInDifferentFile() filetree is missing group '" + groupname + "'", items[1].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCutPasteGroupInDifferentFile() filetree is missing dataset '" + datasetname + "'",
                    items[1].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[1].getNode(0).click();
            items[1].getNode(0).getNode(0).contextMenu().contextMenu("Open").click();
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
                    assertTrue(constructWrongValueMessage("testCutPasteGroupInDifferentFile()", "wrong data", expected, val), val.equals(expected));
                }
            }

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
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.close();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file2, true);
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
            }
        }
    }

    @Test
    public void testCopyPasteDatasetInSameFile() {
        String filename = "testcopypaste.h5";
        String group_copy_name = "test_group_copy";
        String dataset_copy_name = "test_dataset_copy";
        SWTBotShell tableShell = null;

        File hdf_file = createFile(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testCopyPasteDatasetInSameFile()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("testCopyPasteDatasetInSameFile() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(constructWrongValueMessage("testCopyPasteDatasetInSameFile()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 3);
            assertTrue("testCopyPasteDatasetInSameFile() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCopyPasteDatasetInSameFile() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[0].getNode(0).getNode(0).click();
            items[0].getNode(0).getNode(0).contextMenu().contextMenu("Copy").click();

            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Group").click();

            SWTBotShell groupShell = bot.shell("New Group...");
            groupShell.activate();
            bot.waitUntil(Conditions.shellIsActive(groupShell.getText()));

            groupShell.bot().text(0).setText(group_copy_name);

            String val = groupShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testCopyPasteDatasetInSameFile()", "wrong group name", group_copy_name, val), val.equals(group_copy_name));

            groupShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(groupShell));

            assertTrue(constructWrongValueMessage("testCopyPasteDatasetInSameFile()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 4);
            assertTrue("testCopyPasteDatasetInSameFile() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
            assertTrue("testCopyPasteDatasetInSameFile() filetree is missing group '" + group_copy_name + "'", items[0].getNode(1).getText().compareTo(group_copy_name) == 0);

            items[0].getNode(1).click();
            items[0].getNode(1).contextMenu().contextMenu("Paste").click();

            SWTBotShell copyTargetShell = bot.shells()[1];
            copyTargetShell.activate();
            copyTargetShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(copyTargetShell));

            // Reload file
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(constructWrongValueMessage("testCopyPasteDatasetInSameFile()", "filetree wrong row count", "5", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 5);
            assertTrue("testCopyPasteDatasetInSameFile() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
            assertTrue("testCopyPasteDatasetInSameFile() filetree is missing group '" + group_copy_name + "'", items[0].getNode(1).getText().compareTo(group_copy_name) == 0);

            items[0].getNode(1).click();
            items[0].getNode(1).getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table2 = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            for (int row = 1; row <= table2.preferredRowCount() - 1; row++) {
                for (int col = 1; col < table2.preferredColumnCount(); col++) {
                    String expected = String.valueOf(((row - 1) * (table2.preferredColumnCount() - 1)) + (col));
                    String tval = table2.getCellDataValueByPosition(row, col);
                    assertTrue(constructWrongValueMessage("testCopyPasteDatasetInSameFile()", "wrong data", expected, tval), tval.equals(expected));
                }
            }

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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu().menu("Table").menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }

    @Test
    public void testCopyPasteDatasetInDifferentFile() {
        String filename = "testcopy.h5";
        String filenameTo = "testpaste.h5";
        String group_copy_name = "test_group_copy";
        SWTBotShell tableShell = null;

        File hdf_file = createFile(filename);
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            assertTrue(constructWrongValueMessage("testCopyPasteDatasetInDifferentFile()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("testCopyPasteDatasetInDifferentFile() HDF filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }

        File hdf_file2 = createFile(filenameTo);
        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            assertTrue(constructWrongValueMessage("testCopyPasteDatasetInDifferentFile()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 2);
            assertTrue("testCopyPasteDatasetInDifferentFile() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
            assertTrue("testCopyPasteDatasetInDifferentFile() filetree is missing file '" + filenameTo + "'", items[1].getText().compareTo(filenameTo) == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
            try {
                if (hdf_file.exists())
                    closeFile(hdf_file, true);
            }
            catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
            try {
                if (hdf_file.exists())
                    closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            createNewHDF5Group();
            createNewHDF5Dataset();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(constructWrongValueMessage("testCopyPasteDatasetInDifferentFile()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 4);
            assertTrue("testCopyPasteDatasetInDifferentFile() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCopyPasteDatasetInDifferentFile() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[0].getNode(0).getNode(0).click();
            items[0].getNode(0).getNode(0).contextMenu().contextMenu("Copy").click();

            items[1].click();
            items[1].contextMenu().contextMenu("New").menu("Group").click();

            SWTBotShell groupShell = bot.shell("New Group...");
            groupShell.activate();
            bot.waitUntil(Conditions.shellIsActive(groupShell.getText()));

            groupShell.bot().text(0).setText(group_copy_name);

            String val = groupShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testCutPasteDatasetInSameFile()", "wrong group name", group_copy_name, val), val.equals(group_copy_name));

            groupShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(groupShell));

            items[1].getNode(0).click();
            items[1].getNode(0).contextMenu().contextMenu("Paste").click();

            SWTBotShell copyTargetShell = bot.shells()[1];
            copyTargetShell.activate();
            copyTargetShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(copyTargetShell));

            // Reload file
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            // Reload file
            items[1].click();
            items[1].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);
            filetree.expandNode(items[1].getText(), true);

            assertTrue(constructWrongValueMessage("testCopyPasteDatasetInDifferentFile()", "filetree wrong row count", "6", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 6);
            assertTrue("testCopyPasteDatasetInDifferentFile() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCopyPasteDatasetInDifferentFile() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);
            assertTrue("testCopyPasteDatasetInDifferentFile() filetree is missing group '" + group_copy_name + "'", items[1].getNode(0).getText().compareTo(group_copy_name) == 0);
            assertTrue("testCopyPasteDatasetInDifferentFile() filetree is missing dataset '" + datasetname + "'",
                    items[1].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[1].getNode(0).click();
            items[1].getNode(0).getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table2 = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            for (int row = 1; row <= table2.preferredRowCount() - 1; row++) {
                for (int col = 1; col < table2.preferredColumnCount(); col++) {
                    String expected = String.valueOf(((row - 1) * (table2.preferredColumnCount() - 1)) + (col));
                    String tval = table2.getCellDataValueByPosition(row, col);
                    assertTrue(constructWrongValueMessage("testCopyPasteDatasetInDifferentFile()", "wrong data", expected, tval), tval.equals(expected));
                }
            }

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
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.close();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file2, true);
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
            }
        }
    }

    @Test
    public void testCutPasteDatasetInSameFile() {
        String filename = "testcopypaste.h5";
        String group_copy_name = "test_group_copy";
        String dataset_copy_name = "test_dataset_copy";
        SWTBotShell tableShell = null;

        File hdf_file = createFile(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testCutPasteDatasetInSameFile()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("testCutPasteDatasetInSameFile() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(constructWrongValueMessage("testCutPasteDatasetInSameFile()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 3);
            assertTrue("testCutPasteDatasetInSameFile() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testCutPasteDatasetInSameFile() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[0].getNode(0).getNode(0).click();
            items[0].getNode(0).getNode(0).contextMenu().contextMenu("Cut").click();

            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Group").click();

            SWTBotShell groupShell = bot.shell("New Group...");
            groupShell.activate();
            bot.waitUntil(Conditions.shellIsActive(groupShell.getText()));

            groupShell.bot().text(0).setText(group_copy_name);

            String val = groupShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testCutPasteDatasetInSameFile()", "wrong group name", group_copy_name, val), val.equals(group_copy_name));

            groupShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(groupShell));

            items[0].getNode(1).click();
            items[0].getNode(1).contextMenu().contextMenu("Paste").click();

            SWTBotShell copyTargetShell = bot.shells()[1];
            copyTargetShell.activate();
            copyTargetShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(copyTargetShell));

            // Reload file
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(constructWrongValueMessage("testCutPasteDatasetInSameFile()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 4);
            assertTrue("testCutPasteDatasetInSameFile() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename) == 0);
            assertTrue("testCutPasteDatasetInSameFile() filetree is missing group '" + group_copy_name + "'", items[0].getNode(1).getText().compareTo(group_copy_name) == 0);

            items[0].getNode(1).getNode(0).click();
            items[0].getNode(1).getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table2 = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            for (int row = 1; row <= table2.preferredRowCount() - 1; row++) {
                for (int col = 1; col < table2.preferredColumnCount(); col++) {
                    String expected = String.valueOf(((row - 1) * (table2.preferredColumnCount() - 1)) + (col));
                    String tval = table2.getCellDataValueByPosition(row, col);
                    assertTrue(constructWrongValueMessage("testCopyPasteDatasetInSameFile()", "wrong data", expected, tval), tval.equals(expected));
                }
            }

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
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu().menu("Table").menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
            }
        }
    }
}
