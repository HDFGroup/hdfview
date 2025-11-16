package uitest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

import java.io.File;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

@Tag("ui")
@Tag("integration")
public class TestHDFViewLinks extends AbstractWindowTest {
    private String groupname   = "test_group";
    private String datasetname = "test_dataset";

    private void createNewHDF5Group()
    {
        SWTBotShell groupShell = null;

        try {
            SWTBotTree filetree    = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Group").click();

            groupShell = bot.shell("New Group...");
            groupShell.activate();
            bot.waitUntil(Conditions.shellIsActive(groupShell.getText()));

            groupShell.bot().text(0).setText(groupname);

            String val = groupShell.bot().text(0).getText();
            assertTrue(val.equals(groupname),
                constructWrongValueMessage("createNewHDF5Group()", "wrong group name", groupname, val));

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

    private void createNewHDF5Dataset()
    {
        String currentSize       = "4 x 4";
        SWTBotShell tableShell   = null;
        SWTBotShell datasetShell = null;

        try {
            SWTBotTree filetree    = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("New").menu("Dataset").click();

            datasetShell = bot.shell("New Dataset...");
            datasetShell.activate();
            bot.waitUntil(Conditions.shellIsActive(datasetShell.getText()));

            datasetShell.bot().text(0).setText(datasetname);

            String val = datasetShell.bot().text(0).getText();
            assertTrue(val.equals(datasetname),
                constructWrongValueMessage("createNewHDF5Dataset()", "wrong dataset name", datasetname, val));

            datasetShell.bot().text(2).setText(currentSize);

            val = datasetShell.bot().text(2).getText();
            assertTrue(val.equals(currentSize),
                constructWrongValueMessage("createNewHDF5Dataset()", "wrong current size", currentSize, val));

            datasetShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(datasetShell));

            filetree.expandNode(items[0].getText(), true);
            items = filetree.getAllItems();

            items[0].getNode(0).getNode(0).click();
            items[0].getNode(0).getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher =
                WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table =
                new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run()
                {
                    for (int row = 1; row <= 4; row++) {
                        for (int col = 1; col <= 4; col++) {
                            String val = String.valueOf(((row - 1) * 4) + (col));
                            table.doubleclick(row, col);
                            table.widget.getActiveCellEditor().setEditorValue(val);
                            table.widget.getActiveCellEditor().commit(SelectionLayer.MoveDirectionEnum.RIGHT,
                                                                      true, true);
                            assertTrue(table.getCellDataValueByPosition(row, col).equals(val),
                                constructWrongValueMessage("createNewHDF5Dataset()", "wrong value",
                                                                  val,
                                                                  table.getCellDataValueByPosition(row, col)));
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
    public void testHardLinks()
    {
        String filename          = "testhardlinks.h5";
        String group_link_name   = "test_group_link";
        String dataset_link_name = "test_dataset_link";
        SWTBotShell tableShell   = null;

        File hdf_file = createFile(filename);

        try {
            SWTBotTree filetree    = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(filetree.visibleRowCount() == 1,
                constructWrongValueMessage("testHardLinks()", "filetree wrong row count", "1",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getText().compareTo(filename) == 0,
                "testHardLinks() filetree is missing file '" + filename + "'");

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(filetree.visibleRowCount() == 3,
                constructWrongValueMessage("testHardLinks()", "filetree wrong row count", "3",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getNode(0).getText().compareTo(groupname) == 0,
                "testHardLinks() filetree is missing group '" + groupname + "'");
            assertTrue(items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0,
                "testHardLinks() filetree is missing dataset '" + datasetname + "'");

            // Test links to groups
            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Link").click();

            SWTBotShell linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText(group_link_name);

            String val = linkShell.bot().text(0).getText();
            assertTrue(val.equals(group_link_name),
                constructWrongValueMessage("testHardLinks()", "wrong link name", group_link_name, val));

            SWTBotCombo combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(val.equals("/"),
                constructWrongValueMessage("testHardLinks()", "wrong link parent", "/", val));

            linkShell.bot().radio("Hard Link").click();

            SWTBotCCombo ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setSelection("/" + groupname + "/");

            val = ccombo.getText();
            assertTrue(val.equals("/" + groupname + "/"),
                constructWrongValueMessage("testHardLinks()", "wrong link target",
                                                  "/" + groupname + "/", val));

            linkShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File As").menu("Read/Write").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(filetree.visibleRowCount() == 5,
                constructWrongValueMessage("testHardLinks()", "filetree wrong row count", "5",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getNode(1).getText().compareTo(group_link_name) == 0,
                "testHardLinks() filetree is missing link '" + group_link_name + "'");

            // Delete link
            items[0].getNode(1).click();
            items[0].getNode(1).contextMenu().contextMenu("Delete").click();

            SWTBotShell dialog = bot.shells()[1];
            dialog.activate();
            dialog.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(dialog));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File As").menu("Read/Write").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            // Test links to datasets
            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Link").click();

            linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText(dataset_link_name);

            val = linkShell.bot().text(0).getText();
            assertTrue(val.equals(dataset_link_name),
                constructWrongValueMessage("testHardLinks()", "wrong link name", dataset_link_name, val));

            combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(val.equals("/"),
                constructWrongValueMessage("testHardLinks()", "wrong link parent", "/", val));

            linkShell.bot().radio("Hard Link").click();

            ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setSelection("/" + groupname + "/" + datasetname);

            val = ccombo.getText();
            assertTrue(val.equals("/" + groupname + "/" + datasetname),
                constructWrongValueMessage("testHardLinks()", "wrong link target",
                                                  "/" + groupname + "/" + datasetname, val));

            linkShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(filetree.visibleRowCount() == 4,
                constructWrongValueMessage("testHardLinks()", "filetree wrong row count", "4",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getNode(0).getText().compareTo(dataset_link_name) == 0,
                "testHardLinks() filetree is missing link '" + dataset_link_name + "'");

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher =
                WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            // Verify cell data is correct
            final SWTBotNatTable table =
                new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            for (int row = 1; row <= 4; row++) {
                for (int col = 1; col <= 4; col++) {
                    String thisVal  = table.getCellDataValueByPosition(row, col);
                    String expected = String.valueOf(((row - 1) * 4) + (col));
                    assertTrue(thisVal.equals(expected),
                        constructWrongValueMessage("testHardLinks()", "wrong data", expected, thisVal));
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testSoftLinks()
    {
        String filename          = "testsoftlinks.h5";
        String group_link_name   = "test_group_link";
        String dataset_link_name = "test_dataset_link";
        SWTBotShell tableShell   = null;

        File hdf_file = createFile(filename);

        try {
            SWTBotTree filetree    = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(filetree.visibleRowCount() == 1,
                constructWrongValueMessage("testSoftLinks()", "filetree wrong row count", "1",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getText().compareTo(filename) == 0,
                "testSoftLinks() filetree is missing file '" + filename + "'");

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(filetree.visibleRowCount() == 3,
                constructWrongValueMessage("testSoftLinks()", "filetree wrong row count", "3",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getNode(0).getText().compareTo(groupname) == 0,
                "testSoftLinks() filetree is missing group '" + groupname + "'");
            assertTrue(items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0,
                "testSoftLinks() filetree is missing dataset '" + datasetname + "'");

            // Test soft link to existing object
            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Link").click();

            SWTBotShell linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText(group_link_name);

            String val = linkShell.bot().text(0).getText();
            assertTrue(val.equals(group_link_name),
                constructWrongValueMessage("testSoftLinks()", "wrong link name", group_link_name, val));

            SWTBotCombo combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(val.equals("/"),
                constructWrongValueMessage("testSoftLinks()", "wrong link parent", "/", val));

            linkShell.bot().radio("Soft Link").click();

            SWTBotCCombo ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setSelection("/" + groupname + "/");

            val = ccombo.getText();
            assertTrue(val.equals("/" + groupname + "/"),
                constructWrongValueMessage("testSoftLinks()", "wrong link target",
                                                  "/" + groupname + "/", val));

            linkShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File As").menu("Read/Write").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(filetree.visibleRowCount() == 5,
                constructWrongValueMessage("testSoftLinks()", "filetree wrong row count", "5",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getNode(1).getText().compareTo(group_link_name) == 0,
                "testSoftLinks() filetree is missing link '" + group_link_name + "'");

            // Test soft link to non-existing object
            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Link").click();

            linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText("test_nonexisting_object_link");

            val = linkShell.bot().text(0).getText();
            assertTrue(val.equals("test_nonexisting_object_link"),
                constructWrongValueMessage("testSoftLinks()", "wrong link name",
                                                  "test_nonexisting_object_link", val));

            combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(val.equals("/"),
                constructWrongValueMessage("testSoftLinks()", "wrong link parent", "/", val));

            linkShell.bot().radio("Soft Link").click();

            ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setSelection("/" + groupname + "/");

            val = ccombo.getText();
            assertTrue(val.equals("/" + groupname + "/"),
                constructWrongValueMessage("testSoftLinks()", "wrong link target",
                                                  "/" + groupname + "/", val));

            ccombo.setText("nonexist");

            val = linkShell.bot().ccomboBox().getText();
            assertTrue(val.equals("nonexist"),
                constructWrongValueMessage("testSoftLinks()", "wrong link target", "nonexist", val));

            // linkShell.bot().button(" &Cancel ").click();
            // bot.waitUntil(Conditions.shellCloses(linkShell));

            linkShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File As").menu("Read/Write").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(filetree.visibleRowCount() == 6,
                constructWrongValueMessage("testSoftLinks()", "filetree wrong row count", "6",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getNode(2).getText().compareTo("test_nonexisting_object_link") == 0,
                "testSoftLinks() filetree is missing link 'test_nonexisting_object_link'");

            // Change link target to existing object
            items[0].getNode(2).click();

            SWTBotTabItem tabItem = bot.tabItem("General Object Info");
            tabItem.activate();

            val = bot.textWithLabel("Link To Target: ").getText();
            assertTrue(val.equals("/nonexist"),
                constructWrongValueMessage("testSoftLinks()", "wrong link name", "/nonexist", val));

            // skip the rest untill issue with MessageDialog can be fixed
            //            bot.textWithLabel("Link To Target: ").setText("/" + groupname + "/" +
            //            datasetname).pressShortcut(Keystrokes.TAB);
            //
            //            SWTBotShell linkTargetShell = bot.shell("HDFView " + VERSION + " - Link target
            //            changed."); linkTargetShell.activate();
            //            bot.waitUntil(Conditions.shellIsActive(linkTargetShell.getText()));
            //            linkTargetShell.bot().button("OK").click();
            //            bot.waitUntil(Conditions.shellCloses(linkTargetShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            //            assertTrue(constructWrongValueMessage("testSoftLinks()", "filetree wrong row count",
            //            "6",
            //                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() ==
            //                    6);
            //            assertTrue("testSoftLinks() filetree is missing link
            //            'test_nonexisting_object_link'",
            //                    items[0].getNode(2).getText().compareTo("test_nonexisting_object_link") ==
            //                    0);
            //
            //            items[0].getNode(2).click();
            //            items[0].getNode(2).contextMenu().contextMenu("Open").click();
            //            org.hamcrest.Matcher<Shell> shellMatcher =
            //            WithRegex.withRegex("test_nonexisting_object_link.*at.*\\[.*in.*\\]");
            //            bot.waitUntil(Conditions.waitForShell(shellMatcher));
            //
            //            tableShell = bot.shells()[1];
            //            tableShell.activate();
            //            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            //
            //            final SWTBotNatTable table = new
            //            SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));
            //
            //            for (int row = 1; row <= 4; row++) {
            //                for (int col = 1; col <= 4; col++) {
            //                    String thisVal = table.getCellDataValueByPosition(row, col);
            //                    String expected = String.valueOf(((row - 1) * 4) + (col));
            //                    assertTrue(constructWrongValueMessage("testSoftLinks()", "wrong data",
            //                    expected, thisVal),
            //                            thisVal.equals(expected));
            //                }
            //            }
            //
            //            tableShell.bot().menu().menu("Table").menu("Close").click();
            //            bot.waitUntil(Conditions.shellCloses(tableShell));
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testExternalLinks()
    {
        String filename          = "testexternallinks.h5";
        String file_link_name    = "tintsize.h5";
        String file_dset_name    = "DU08BITS";
        String dataset_link_name = "test_external_dataset_link";
        SWTBotShell tableShell   = null;

        File hdf_file = createFile(filename);

        try {
            SWTBotTree filetree    = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(filetree.visibleRowCount() == 1,
                constructWrongValueMessage("testExternalLinks()", "filetree wrong row count", "1",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getText().compareTo(filename) == 0,
                "testExternalLinks() filetree is missing file '" + filename + "'");

            createNewHDF5Group();
            createNewHDF5Dataset();

            assertTrue(filetree.visibleRowCount() == 3,
                constructWrongValueMessage("testExternalLinks()", "filetree wrong row count", "3",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getNode(0).getText().compareTo(groupname) == 0,
                "testExternalLinks() filetree is missing group '" + groupname + "'");
            assertTrue(items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0,
                "testExternalLinks() filetree is missing dataset '" + datasetname + "'");

            // Test external link to existing object
            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Link").click();

            SWTBotShell linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText(file_link_name);

            String val = linkShell.bot().text(0).getText();
            assertTrue(val.equals(file_link_name),
                constructWrongValueMessage("testExternalLinks()", "wrong link name", file_link_name, val));

            SWTBotCombo combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(val.equals("/"),
                constructWrongValueMessage("testExternalLinks()", "wrong link parent", "/", val));

            linkShell.bot().radio("External Link").click();

            linkShell.bot().textWithLabel("Target File: ").setText(workDir + "/" + file_link_name);

            val = linkShell.bot().textWithLabel("Target File: ").getText();
            assertTrue(val.equals(workDir + "/" + file_link_name),
                constructWrongValueMessage("testExternalLinks()", "wrong link file",
                                                  workDir + "/" + file_link_name, val));

            SWTBotCCombo ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setText("/" + file_dset_name);

            val = ccombo.getText();
            assertTrue(val.equals("/" + file_dset_name),
                constructWrongValueMessage("testExternalLinks()", "wrong link target",
                                                  "/" + file_dset_name, val));

            linkShell.bot().button("   OK   ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File As").menu("Read/Write").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(filetree.visibleRowCount() == 4,
                constructWrongValueMessage("testExternalLinks()", "filetree wrong row count", "4",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getNode(1).getText().compareTo(file_link_name) == 0,
                "testExternalLinks() filetree is missing link '" + file_link_name + "'");

            items[0].getNode(1).click();
            items[0].getNode(1).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher =
                WithRegex.withRegex(file_link_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            table.click(1, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(val.equals("255"),
                constructWrongValueMessage("testExternalLinks()", "wrong data", "255", val));

            table.click(8, 1);
            val = tableShell.bot().text(0).getText();
            assertTrue(val.equals("128"),
                constructWrongValueMessage("testExternalLinks()", "wrong data", "128", val));

            // TODO Disabled until offscreen columns/rows can be accessed
            // table.click(8, 8);
            // val = tableShell.bot().text(0).getText();
            // assertTrue(constructWrongValueMessage("testExternalLinks()", "wrong data", "0", val),
            // val.equals("0"));

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            // Test external link to non-existing object
            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Link").click();

            linkShell = bot.shell("New Link...");
            linkShell.activate();
            bot.waitUntil(Conditions.shellIsActive(linkShell.getText()));

            linkShell.bot().text(0).setText("test_external_nonexisting_link");

            val = linkShell.bot().text(0).getText();
            assertTrue(val.equals("test_external_nonexisting_link"),
                constructWrongValueMessage("testExternalLinks()", "wrong link name",
                                                  "test_external_nonexisting_link", val));

            combo = linkShell.bot().comboBox(0);
            combo.setSelection("/");

            val = combo.getText();
            assertTrue(val.equals("/"),
                constructWrongValueMessage("testExternalLinks()", "wrong link parent", "/", val));

            linkShell.bot().radio("External Link").click();

            linkShell.bot().textWithLabel("Target File: ").setText(workDir + "/" + file_link_name);

            val = linkShell.bot().textWithLabel("Target File: ").getText();
            assertTrue(val.equals(workDir + "/" + file_link_name),
                constructWrongValueMessage("testExternalLinks()", "wrong link file",
                                                  workDir + "/" + file_link_name, val));

            ccombo = linkShell.bot().ccomboBox(0);
            ccombo.setText("/nonexist");

            val = ccombo.getText();
            assertTrue(val.equals("/nonexist"),
                constructWrongValueMessage("testExternalLinks()", "wrong link target", "/nonexist", val));

            linkShell.bot().button("   OK   ").click();
            bot.waitUntil(Conditions.shellCloses(linkShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File As").menu("Read/Write").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            assertTrue(filetree.visibleRowCount() == 5,
                constructWrongValueMessage("testExternalLinks()", "filetree wrong row count", "5",
                                                  String.valueOf(filetree.visibleRowCount())));
            assertTrue(items[0].getNode(0).getText().compareTo("test_external_nonexisting_link") == 0,
                "testExternalLinks() filetree is missing link '"
                           + "test_external_nonexisting_link"
                           + "'");

            // Change link target to existing object
            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("General Object Info");
            tabItem.activate();

            val             = bot.textWithLabel("Link To Target: ").getText();
            int targetIndex = val.lastIndexOf(':');
            String target   = val.substring(0, targetIndex) + ":///DU32BITS";
            //            bot.textWithLabel("Link To Target: ").setText(target).pressShortcut(Keystrokes.CR);
            //
            //            SWTBotShell linkTargetShell = bot.shell("HDFView " + VERSION + " - Link target
            //            changed."); linkTargetShell.activate();
            //            bot.waitUntil(Conditions.shellIsActive(linkTargetShell.getText()));
            //            linkTargetShell.bot().button("OK").click();
            //            bot.waitUntil(Conditions.shellCloses(linkTargetShell));

            // Reload file to update link
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            filetree.expandNode(items[0].getText(), true);

            //            assertTrue(constructWrongValueMessage("testExternalLinks()", "filetree wrong row
            //            count", "5", String.valueOf(filetree.visibleRowCount())),
            //                    filetree.visibleRowCount() == 5);
            //            assertTrue("testExternalLinks() filetree is missing link
            //            'test_external_nonexisting_link'",
            //                    items[0].getNode(0).getText().compareTo("test_external_nonexisting_link") ==
            //                    0);
            //
            //            items[0].getNode(0).click();
            //            items[0].getNode(0).contextMenu().contextMenu("Open").click();
            //            shellMatcher =
            //            WithRegex.withRegex("test_external_nonexisting_link.*at.*\\[.*in.*\\]");
            //            bot.waitUntil(Conditions.waitForShell(shellMatcher));
            //
            //            tableShell = bot.shells()[1];
            //            tableShell.activate();
            //            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            //
            //            table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));
            //
            //            table.click(1, 1);
            //            val = tableShell.bot().text(0).getText();
            //            assertTrue(constructWrongValueMessage("testExternalLinks()", "wrong data",
            //            "4294967295", val), val.equals("4294967295"));
            //
            //            table.click(8, 1);
            //            val = tableShell.bot().text(0).getText();
            //            assertTrue(constructWrongValueMessage("testExternalLinks()", "wrong data",
            //            "4294967168", val), val.equals("4294967168"));
            //
            //            table.click(8, 8);
            //            val = tableShell.bot().text(0).getText();
            //            assertTrue(constructWrongValueMessage("testExternalLinks()", "wrong data",
            //            "4294950912", val), val.equals("4294950912"));
            //
            //            tableShell.bot().menu().menu("Table").menu("Close").click();
            //            bot.waitUntil(Conditions.shellCloses(tableShell));
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
