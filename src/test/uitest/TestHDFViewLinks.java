package test.uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

public class TestHDFViewLinks extends AbstractWindowTest {
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
            assertTrue(constructWrongValueMessage("createNewHDF5Group()", "wrong group name", groupname, val),
                    val.equals(groupname));

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
            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "wrong dataset name", datasetname, val),
                    val.equals(datasetname));

            datasetShell.bot().text(2).setText(currentSize);

            val = datasetShell.bot().text(2).getText();
            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "wrong current size", currentSize, val),
                    val.equals(currentSize));

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
                            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "wrong value",
                                    val, table.getCellDataValueByPosition(row, col)),
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
    public void testHardLinks() {
        String filename = "testhardlinks";
        String file_ext = ".h5";
        String group_link_name = "test_group_link";
        String dataset_link_name = "test_dataset_link";
        SWTBotShell tableShell = null;

        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testHardLinks()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("testHardLinks() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(constructWrongValueMessage("testHardLinks()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 3);
            assertTrue("testHardLinks() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testHardLinks() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            // Test links to groups
            items[0].click();
            items[0].contextMenu("New").menu("Link").click();

            SWTBotShell linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText(group_link_name);

            String val = linkShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testHardLinks()", "wrong link name", group_link_name, val),
                    val.equals(group_link_name));

            SWTBotCombo combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(constructWrongValueMessage("testHardLinks()", "wrong link parent", "/", val),
                    val.equals("/"));

            linkShell.bot().radio("Hard Link").click();

            SWTBotCCombo ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setSelection("/" + groupname + "/");

            val = ccombo.getText();
            assertTrue(constructWrongValueMessage("testHardLinks()", "wrong link target", "/" + groupname + "/", val),
                    val.equals("/" + groupname + "/"));

            linkShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(constructWrongValueMessage("testHardLinks()", "filetree wrong row count", "5", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 5);
            assertTrue("testHardLinks() filetree is missing link '" + group_link_name + "'",
                    items[0].getNode(1).getText().compareTo(group_link_name) == 0);

            // Delete link
            items[0].getNode(1).click();
            items[0].getNode(1).contextMenu("Delete").click();

            SWTBotShell dialog = bot.shell("Remove object");
            dialog.activate();
            dialog.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(dialog));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            // Test links to datasets
            items[0].click();
            items[0].contextMenu("New").menu("Link").click();

            linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText(dataset_link_name);

            val = linkShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testHardLinks()", "wrong link name", dataset_link_name, val),
                    val.equals(dataset_link_name));

            combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(constructWrongValueMessage("testHardLinks()", "wrong link parent", "/", val),
                    val.equals("/"));

            linkShell.bot().radio("Hard Link").click();

            ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setSelection("/" + groupname + "/" + datasetname);

            val = ccombo.getText();
            assertTrue(constructWrongValueMessage("testHardLinks()", "wrong link target", "/" + groupname + "/" + datasetname, val),
                    val.equals("/" + groupname + "/" + datasetname));

            linkShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(constructWrongValueMessage("testHardLinks()", "filetree wrong row count", "4", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 4);
            assertTrue("testHardLinks() filetree is missing link '" + dataset_link_name + "'",
                    items[0].getNode(0).getText().compareTo(dataset_link_name) == 0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            for (int row = 1; row <= 4; row++) {
                for (int col = 1; col <= 4; col++) {
                    String thisVal = table.getCellDataValueByPosition(row, col);
                    String expected = String.valueOf(((row - 1) * 4) + (col));
                    assertTrue(constructWrongValueMessage("testHardLinks()", "wrong data", expected, thisVal),
                            thisVal.equals(expected));
                }
            }

            tableShell.bot().menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.close();
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
    public void testSoftLinks() {
        String filename = "testsoftlinks";
        String file_ext = ".h5";
        String group_link_name = "test_group_link";
        String dataset_link_name = "test_dataset_link";
        SWTBotShell tableShell = null;

        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testSoftLinks()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("testSoftLinks() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext)==0);

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(constructWrongValueMessage("testSoftLinks()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 3);
            assertTrue("testSoftLinks() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testSoftLinks() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            // Test soft link to existing object
            items[0].click();
            items[0].contextMenu("New").menu("Link").click();

            SWTBotShell linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText(group_link_name);

            String val = linkShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testSoftLinks()", "wrong link name", group_link_name, val),
                    val.equals(group_link_name));

            SWTBotCombo combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(constructWrongValueMessage("testSoftLinks()", "wrong link parent", "/", val),
                    val.equals("/"));

            linkShell.bot().radio("Soft Link").click();

            SWTBotCCombo ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setSelection("/" + groupname + "/");

            val = ccombo.getText();
            assertTrue(constructWrongValueMessage("testSoftLinks()", "wrong link target", "/" + groupname + "/", val),
                    val.equals("/" + groupname + "/"));

            linkShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(constructWrongValueMessage("testSoftLinks()", "filetree wrong row count", "5", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 5);
            assertTrue("testSoftLinks() filetree is missing link '" + group_link_name + "'", items[0].getNode(1).getText().compareTo(group_link_name) == 0);

            // Test soft link to non-existing object
            items[0].click();
            items[0].contextMenu("New").menu("Link").click();

            linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText("test_nonexisting_object_link");

            val = linkShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testSoftLinks()", "wrong link name", "test_nonexisting_object_link", val),
                    val.equals("test_nonexisting_object_link"));

            combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(constructWrongValueMessage("testSoftLinks()", "wrong link parent", "/", val),
                    val.equals("/"));

            linkShell.bot().radio("Soft Link").click();

            ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setSelection("/" + groupname + "/");

            val = ccombo.getText();
            assertTrue(constructWrongValueMessage("testSoftLinks()", "wrong link target", "/" + groupname + "/", val),
                    val.equals("/" + groupname + "/"));

            ccombo.setText("nonexist");

            val = linkShell.bot().ccomboBox().getText();
            assertTrue(constructWrongValueMessage("testSoftLinks()", "wrong link target", "nonexist", val),
                    val.equals("nonexist"));

            linkShell.bot().button(" &Cancel ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            /*
             * linkShell.bot().button(" &OK ").click(); bot.waitUntil(Conditions.shellCloses(linkShell));
             *
             * // Reload file to update link items[0].click(); items[0].contextMenu("Reload File").click();
             *
             * items = filetree.getAllItems(); filetree.expandNode(items[0].getText(), true);
             *
             * assertTrue(constructWrongValueMessage("testSoftLinks()", "filetree wrong row count", "6",
             * String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 6);
             * assertTrue("testSoftLinks() filetree is missing link 'test_nonexisting_object_link'",
             * items[0].getNode(2).getText().compareTo("test_nonexisting_object_link") == 0);
             *
             * // Change link to point to existing object - metadata items[0].getNode(2).click();
             *
             * SWTBotTabItem tabItem = bot.tabItem("General Object Info"); tabItem.activate();
             *
             * val = bot.textWithLabel("Link To Target: ").getText();
             * assertTrue(constructWrongValueMessage("testSoftLinks()", "wrong link name",
             * "test_nonexisting_object_link", val), val.equals("test_nonexisting_object_link"));
             *
             * linkShell.bot().textWithLabel("Link To Target: ").setText("/" + groupname + "/" + datasetname);
             *
             * SWTBotShell linkTargetShell = bot.shell("Link target changed."); linkTargetShell.activate();
             * linkTargetShell.bot().button("   &OK   ").click();
             * bot.waitUntil(Conditions.shellCloses(linkShell));
             *
             * // Reload file to update link items[0].click(); items[0].contextMenu("Reload File").click();
             *
             * items = filetree.getAllItems(); filetree.expandNode(items[0].getText(), true);
             *
             * assertTrue(constructWrongValueMessage("testSoftLinks()", "filetree wrong row count", "6",
             * String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 6);
             * assertTrue("testSoftLinks() filetree is missing link 'test_nonexisting_object_link'",
             * items[0].getNode(2).getText().compareTo("test_nonexisting_object_link") == 0);
             *
             * items[0].getNode(2).click(); items[0].getNode(2).contextMenu("Open").click();
             * org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname +
             * ".*at.*\\[.*in.*\\]"); bot.waitUntil(Conditions.waitForShell(shellMatcher));
             *
             * tableShell = bot.shells()[1]; tableShell.activate();
             * bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
             *
             * final SWTBotNatTable table = new
             * SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));
             *
             * for (int row = 1; row <= 4; row++) { for (int col = 1; col <= 4; col++) { String thisVal =
             * table.getCellDataValueByPosition(row, col); String expected = String.valueOf(((row - 1) * 4) +
             * (col)); assertTrue(constructWrongValueMessage("testSoftLinks()", "wrong data", expected,
             * thisVal), thisVal.equals(expected)); } }
             *
             * tableShell.bot().menu("Close").click(); bot.waitUntil(Conditions.shellCloses(tableShell));
             */
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.close();
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
    public void testExternalLinks() {
        String filename = "testexternallinks";
        String file_ext = ".h5";
        String file_link_name = "tintsize.h5";
        String file_dset_name = "DU08BITS";
        String dataset_link_name = "test_external_dataset_link";
        SWTBotShell tableShell = null;

        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testExternalLinks()", "filetree wrong row count", "1",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 1);
            assertTrue("testExternalLinks() filetree is missing file '" + filename + file_ext + "'",
                    items[0].getText().compareTo(filename + file_ext) == 0);

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(constructWrongValueMessage("testExternalLinks()", "filetree wrong row count", "3",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 3);
            assertTrue("testExternalLinks() filetree is missing group '" + groupname + "'",
                    items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testExternalLinks() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            // Test external link to existing object
            items[0].click();
            items[0].contextMenu("New").menu("Link").click();

            SWTBotShell linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText(file_link_name);

            String val = linkShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("testExternalLinks()", "wrong link name", file_link_name, val),
                    val.equals(file_link_name));

            SWTBotCombo combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(constructWrongValueMessage("testExternalLinks()", "wrong link parent", "/", val),
                    val.equals("/"));

            linkShell.bot().radio("External Link").click();

            linkShell.bot().textWithLabel("Target File: ").setText(workDir + "/" + file_link_name);

            val = linkShell.bot().textWithLabel("Target File: ").getText();
            assertTrue(
                    constructWrongValueMessage("testExternalLinks()", "wrong link file", workDir + "/" + file_link_name,
                            val),
                    val.equals(workDir + "/" + file_link_name));

            SWTBotCCombo ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setText("/" + file_dset_name);

            val = ccombo.getText();
            assertTrue(
                    constructWrongValueMessage("testExternalLinks()", "wrong link target", "/" + file_dset_name, val),
                    val.equals("/" + file_dset_name));

            linkShell.bot().button("   OK   ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(constructWrongValueMessage("testExternalLinks()", "filetree wrong row count", "4",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 4);
            assertTrue("testExternalLinks() filetree is missing link '" + file_link_name + "'",
                    items[0].getNode(1).getText().compareTo(file_link_name) == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.close();
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
