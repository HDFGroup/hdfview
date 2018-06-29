package test.uitest;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

//@RunWith(SWTBotJunit4ClassRunner.class)
public class TestHDFViewMenu extends AbstractWindowTest {
    @Test
    public void verifyOpenButtonEnabled() {
        try {
            boolean status = bot.toolbarButtonWithTooltip("Open").isEnabled();
            assertTrue("verifyOpenButtonEnabled() open button not enabled ", status);
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
    public void verifyCloseButtonEnabled() {
        try {
            boolean status = bot.toolbarButtonWithTooltip("Close").isEnabled();
            assertTrue("verifyCloseButtonEnabled() close button not enabled", status);
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
    public void verifyHelpButtonEnabled() {
        try {
            boolean status = bot.toolbarButtonWithTooltip("Help").isEnabled();
            assertTrue("verifyHelpButtonEnabled() help button not enabled", status);
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
    public void verifyHDF4ButtonEnabled() {
        try {
            boolean status = bot.toolbarButtonWithTooltip("HDF4 Library Version").isEnabled();
            assertTrue("verifyHDF4ButtonEnabled() HDF4 button not enabled", status);
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
    public void verifyHDF5ButtonEnabled() {
        try {
            boolean status = bot.toolbarButtonWithTooltip("HDF5 Library Version").isEnabled();
            assertTrue("verifyHDF5ButtonEnabled() HDF5 button not enabled", status);
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
    public void verifyTextInLabelWhenClickingHDF4Button() {
        try {
            bot.toolbarButtonWithTooltip("HDF4 Library Version").click();

            SWTBotShell botshell = bot.shell("HDF Library Version");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("HDF Library Version"));

            String val = botshell.bot().label(1).getText();
            assertTrue(constructWrongValueMessage("verifyTextInLabelWhenClickingHDF4Button()", "wrong label text", HDF4VERSION, val),
                    val.equals(HDF4VERSION));
            botshell.bot().button("   &OK   ").click();
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
    public void verifyTextInLabelWhenClickingHDF5Button() {
        try {
            bot.toolbarButtonWithTooltip("HDF5 Library Version").click();

            SWTBotShell botshell = bot.shell("HDF Library Version");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("HDF Library Version"));

            String val = botshell.bot().label(1).getText();
            assertTrue(constructWrongValueMessage("verifyTextInLabelWhenClickingHDF5Button()", "wrong label text", HDF5VERSION, val),
                    val.equals(HDF5VERSION));

            botshell.bot().label(HDF5VERSION);
            botshell.bot().button("   &OK   ").click();
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
    public void verifyButtonOpen() {
        String filename = "testopenbutton";
        File hdf_file = createHDF4File(filename);

        try {
            closeFile(hdf_file, false);

            openFile(filename, true);
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void verifyButtonClose() {
        String filename = "closebutton";
        String file_ext = ".hdf";
        File hdf_file = createHDF4File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("verifyButtonClose()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("verifyButtonClose() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext) == 0);

            items[0].click();

            bot.toolbarButtonWithTooltip("Close").click();

            assertTrue("verifyButtonClose() file '" + hdf_file + "' not deleted", hdf_file.delete());
            assertFalse("verifyButtonClose() file '" + hdf_file + "' wasn't gone", hdf_file.exists());
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
                if(hdf_file.exists())
                    closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void verifyMenuOpen() {
        String filename = "testopenfile";
        String file_ext = ".hdf";
        File hdf_file = createHDF4File(filename);

        try {
            closeFile(hdf_file, false);

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Open");
            fileMenuItem.click();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive(shell.getText()));

            SWTBotText text = shell.bot().text();
            text.setText(filename + file_ext);

            String val = text.getText();
            assertTrue(constructWrongValueMessage("verifyMenuOpen()", "wrong file name", filename + file_ext, val),
                    val.equals(filename + file_ext));

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            SWTBotTree filetree = bot.tree();
            assertTrue(constructWrongValueMessage("verifyMenuOpen()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("verifyMenuOpen() filetree is missing file '" + filename + file_ext + "'", filetree.getAllItems()[0].getText().compareTo(filename + file_ext) == 0);
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void verifyMenuOpenReadOnly() {
        String filename = "testopenrofile";
        String file_ext = ".h5";
        File hdf_file = createHDF5File(filename);

        try {
            closeFile(hdf_file, false);

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Open As").menu("Read-Only");
            fileMenuItem.click();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive(shell.getText()));

            SWTBotText text = shell.bot().text();
            text.setText(filename + file_ext);

            String val = text.getText();
            assertTrue(constructWrongValueMessage("verifyMenuOpenReadOnly()", "wrong file name", filename + file_ext, val),
                    val.equals(filename + file_ext));

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            assertTrue(constructWrongValueMessage("verifyMenuOpenReadOnly()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("verifyMenuOpenReadOnly() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext) == 0);

            items[0].click();

            assertFalse("verifyMenuOpenReadOnly() error: New Menu Item is enabled.", items[0].contextMenu("New").isEnabled());
            assertFalse("verifyMenuOpenReadOnly() error: Cut Menu Item is enabled.", items[0].contextMenu("Cut").isEnabled());
            assertFalse("verifyMenuOpenReadOnly() error: Paste Menu Item is enabled.", items[0].contextMenu("Paste").isEnabled());
            assertFalse("verifyMenuOpenReadOnly() error: Delete Menu Item is enabled.", items[0].contextMenu("Delete").isEnabled());
            assertFalse("verifyMenuOpenReadOnly() error: Rename Menu Item is enabled.", items[0].contextMenu("Rename").isEnabled());
            assertFalse("verifyMenuOpenReadOnly() error: Set Lib Version Bounds Menu Item is enabled.", items[0].contextMenu("Set Lib version bounds").isEnabled());
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void verifyMenuNewHDF4() {
        String filename = "testfile";
        String file_ext = ".hdf";
        File hdf_file = null;

        try {
            SWTBotMenu fileMenuItem = bot.menu("File").menu("New").menu("HDF4");
            fileMenuItem.click();

            hdf_file = new File(workDir, filename + file_ext);
            if (hdf_file.exists())
                hdf_file.delete();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive(shell.getText()));

            SWTBotText text = shell.bot().text();
            text.setText(filename + file_ext);

            String val = text.getText();
            assertTrue(constructWrongValueMessage("verifyMenuNewHDF4()", "wrong file name", filename + file_ext, val),
                    val.equals(filename + file_ext));

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            SWTBotTree filetree = bot.tree();
            assertTrue(constructWrongValueMessage("verifyMenuNewHDF4()", "filetree wrong row count", "1", val),
                    filetree.visibleRowCount() == 1);
            assertTrue("verifyMenuNewHDF4() filetree is missing file '" + filename + file_ext + "'", filetree.getAllItems()[0].getText().compareTo(filename + file_ext) == 0);
            assertTrue("verifyMenuNewHDF4() file '" + hdf_file + "' not created", hdf_file.exists());
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void verifyMenuNewHDF5() {
        String filename = "testfile";
        String file_ext = ".h5";
        File hdf_file = null;

        try {
            SWTBotMenu fileMenuItem = bot.menu("File").menu("New").menu("HDF5");
            fileMenuItem.click();

            hdf_file = new File(workDir, filename + file_ext);
            if (hdf_file.exists())
                hdf_file.delete();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive(shell.getText()));

            SWTBotText text = shell.bot().text();
            text.setText(filename + file_ext);

            String val = text.getText();
            assertTrue(constructWrongValueMessage("verifyMenuNewHDF5()", "wrong file name", filename + file_ext, val),
                    val.equals(filename + file_ext));

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            SWTBotTree filetree = bot.tree();
            assertTrue(constructWrongValueMessage("verifyMenuNewHDF5()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("verifyMenuNewHDF5() filetree is missing file '" + filename + file_ext + "'", filetree.getAllItems()[0].getText().compareTo(filename + file_ext) == 0);
            assertTrue("verifyMenuNewHDF5() file '" + hdf_file + "' not created", hdf_file.exists());
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void verifyMenuClose() {
        String filename = "closefile";
        String file_ext = ".hdf";
        File hdf_file = createHDF4File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            assertTrue(constructWrongValueMessage("verifyMenuClose()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("verifyMenuClose() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext) == 0);

            items[0].click();

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Close");
            fileMenuItem.click();

            assertTrue("verifyMenuClose() file '" + hdf_file + "' not deleted", hdf_file.delete());
            assertFalse("verifyMenuClose() file '" + hdf_file + "' not gone", hdf_file.exists());
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
                if(hdf_file.exists())
                    closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void verifyMenuCloseAll() {
        String filename = "closeallfiles";
        String hdf4_file_ext = ".hdf";
        String hdf5_file_ext = ".h5";
        File hdf4_file = createHDF4File(filename);
        File hdf5_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            assertTrue(constructWrongValueMessage("verifyMenuCloseAll()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 2);
            assertTrue("verifyMenuCloseAll() HDF filetree is missing file '" + filename + hdf4_file_ext + "'", items[0].getText().compareTo(filename + hdf4_file_ext) == 0);
            assertTrue("verifyMenuCloseAll() HDF5 filetree is missing file '" + filename + hdf5_file_ext + "'", items[1].getText().compareTo(filename + hdf5_file_ext) == 0);

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Close All");
            fileMenuItem.click();

            assertTrue("verifyMenuCloseAll() HDF file '" + hdf4_file + "' not deleted", hdf4_file.delete());
            assertFalse("verifyMenuCloseAll() HDF file '" + hdf4_file + "' not gone", hdf4_file.exists());

            assertTrue("verifyMenuCloseAll() HDF5 file '" + hdf5_file + "' not deleted", hdf5_file.delete());
            assertFalse("verifyMenuCloseAll() HDF5 file '" + hdf5_file + "' not gone", hdf5_file.exists());
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
                if(hdf4_file.exists())
                    closeFile(hdf4_file, true);
                if(hdf5_file.exists())
                    closeFile(hdf5_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void verifyMenuSave() {
        String filename = "testsavefile";
        String groupname = "grouptestname";
        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].click();

            SWTBotMenu groupMenuItem = items[0].contextMenu("New").menu("Group");
            groupMenuItem.click();

            SWTBotShell botshell = bot.shell("New Group...");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive(botshell.getText()));

            botshell.bot().text(0).setText(groupname);
            botshell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(botshell));

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    shell.forceActive();
                }
            });

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Save");
            fileMenuItem.click();

            closeFile(hdf_file, false);

            openFile(filename, false);

            SWTBotTreeItem group = bot.tree().getAllItems()[0].getNode(0);
            assertTrue("verifyMenuSave() filetree is missing group '" + groupname + "'", group.getText().compareTo(groupname) == 0);
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void verifyMenuSaveAs() {
        String filename = "testsaveasfile";
        String save_to_filename = "testsaveasfile2";
        String file_ext = ".h5";
        String groupname = "grouptestname";

        File hdf_file = createHDF5File(filename);
        File hdf_save_file = new File(workDir, save_to_filename + file_ext);
        if (hdf_save_file.exists())
            hdf_save_file.delete();

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].click();

            SWTBotMenu groupMenuItem = items[0].contextMenu("New").menu("Group");
            groupMenuItem.click();

            SWTBotShell botshell = bot.shell("New Group...");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive(botshell.getText()));

            botshell.bot().text(0).setText(groupname);
            botshell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(botshell));

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    shell.forceActive();
                }
            });

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Save As");
            fileMenuItem.click();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive(shell.getText()));

            SWTBotText text = shell.bot().text();
            text.setText(save_to_filename + file_ext);

            String val = text.getText();
            assertTrue(constructWrongValueMessage("verifyMenuSaveAs()", "wrong file name", save_to_filename + file_ext, val),
                    val.equals(save_to_filename + file_ext));

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            closeFile(hdf_file, true);

            openFile(save_to_filename, false);

            SWTBotTreeItem group = bot.tree().getAllItems()[0].getNode(0);
            assertTrue("verifyMenuSaveAs() filetree is missing group '" + groupname + "'", group.getText().compareTo(groupname) == 0);
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
                if(hdf_file.exists())
                    closeFile(hdf_file, true);
                closeFile(hdf_save_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void verifyMenuWindowCloseAll() {
        String filename = "hdf5_test";
        File hdf_file = null;

        try {
            hdf_file = openFile(filename, false);

            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("verifyMenuWindowCloseAll()", "filetree wrong row count", "6", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 6);
            assertTrue("verifyMenuWindowCloseAll() too many shells open", bot.shells().length == 1);

            filetree.expandNode(filename + ".h5", true);

            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            items[0].getNode(2).getNode(1).click();
            items[0].getNode(2).getNode(1).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            items[0].getNode(2).getNode(2).click();
            items[0].getNode(2).getNode(2).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            items[0].getNode(2).getNode(4).click();
            items[0].getNode(2).getNode(4).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            items[0].getNode(2).getNode(5).click();
            items[0].getNode(2).getNode(5).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            items[0].getNode(2).getNode(6).click();
            items[0].getNode(2).getNode(6).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            items[0].getNode(4).getNode(1).click();
            items[0].getNode(4).getNode(1).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            items[0].getNode(4).getNode(5).click();
            items[0].getNode(4).getNode(5).contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            // Make sure all shells have time to open and the bot has time to
            // stabilize before checking how many shells are open
            bot.sleep(1000);

            assertTrue(constructWrongValueMessage("verifyMenuWindowCloseAll()", "too many or missing shells", "8", String.valueOf(bot.shells().length)),
                    bot.shells().length == 8);

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    shell.forceActive();
                }
            });

            bot.menu("Window").menu("Close All").click();

            assertTrue(constructWrongValueMessage("verifyMenuWindowCloseAll()", "too many or missing shells", "1", String.valueOf(bot.shells().length)),
                    bot.shells().length == 1);
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
    public void verifyTextInLabelWhenClickingHDF4Help() {
        try {
            // Test that the Help->HDF4 Library Version MenuItem works correctly
            SWTBotMenu fileMenuItem = bot.menu("Help").menu("HDF4 Library Version");
            fileMenuItem.click();

            SWTBotShell botshell = bot.shell("HDF Library Version");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("HDF Library Version"));

            String val = botshell.bot().label(1).getText();
            assertTrue(constructWrongValueMessage("verifyTextInLabelWhenClickingHDF4Help()", "wrong label text", HDF4VERSION, val),
                    val.equals(HDF4VERSION));
            botshell.bot().button("   &OK   ").click();
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
    public void verifyTextInLabelWhenClickingHDF5Help() {
        try {
            // Test that the Help->HDF5 Library Version MenuItem works correctly
            SWTBotMenu fileMenuItem = bot.menu("Help").menu("HDF5 Library Version");
            fileMenuItem.click();

            SWTBotShell botshell = bot.shell("HDF Library Version");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("HDF Library Version"));

            String val = botshell.bot().label(1).getText();
            assertTrue(constructWrongValueMessage("verifyTextInLabelWhenClickingHDF5Help()", "wrong label text", HDF5VERSION, val),
                    val.equals(HDF5VERSION));

            botshell.bot().label(HDF5VERSION);
            botshell.bot().button("   &OK   ").click();
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
    public void verifyTextInLabelWhenClickingJavaHelp() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Help").menu("Java Version");
            fileMenuItem.click();

            SWTBotShell botshell = bot.shell("HDFView Java Version");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("HDFView Java Version"));

                    //("Compiled at jdk 1.7.*\\sRunning at.*");
            botshell.bot().button("   &OK   ").click();
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
    public void verifyTextInLabelWhenClickingAboutHelp() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Help").menu("About...");
            fileMenuItem.click();

            SWTBotShell botshell = bot.shell("About HDFView");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("About HDFView"));

                    //("HDF Viewer, Version " + VERSION + "\\sFor.*\\s\\sCopyright.*2006-2016 The HDF Group.\\sAll rights reserved.");
            botshell.bot().button("   &OK   ").click();
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
    public void verifyTextInLabelWhenClickingSupportedFileFormatsHelp() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Help").menu("Supported File Formats");
            fileMenuItem.click();

            SWTBotShell botshell = bot.shell("Supported File Formats");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Supported File Formats"));
                    //("\\sSupported File Formats: \\s.*Fits\\s.*HDF5\\s.*NetCDF\\s.*HDF4\\s\\s");
            botshell.bot().button("   &OK   ").click();
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
    public void verifyRegisterFileFormatTools() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Tools").menu("Unregister File Format");
            fileMenuItem.click();

            SWTBotShell botshell = bot.shell("Unregister a file format");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Unregister a file format"));

            botshell.bot().comboBox().setSelection("FITS");
            botshell.bot().button("   &OK   ").click();

            fileMenuItem = bot.menu("Help").menu("Supported File Formats");
            fileMenuItem.click();

            botshell = bot.shell("Supported File Formats");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Supported File Formats"));
                    //("\\sSupported File Formats: \\s.*HDF5\\s.*NetCDF\\s.*HDF4\\s\\s"));
            botshell.bot().button("   &OK   ").click();

            fileMenuItem = bot.menu("Tools").menu("Register File Format");
            fileMenuItem.click();

            botshell= bot.shell("Register a file format");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Register a file format"));

            botshell.bot().text().setText("FITS:hdf.object.fits.FitsFile:fits");
            botshell.bot().button("   &OK   ").click();

            fileMenuItem = bot.menu("Help").menu("Supported File Formats");
            fileMenuItem.click();

            botshell = bot.shell("Supported File Formats");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Supported File Formats"));
                    //("\\sSupported File Formats: \\s.*Fits\\s.*HDF5\\s.*NetCDF\\s.*HDF4\\s\\s");
            botshell.bot().button("   &OK   ").click();
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
    public void verifyUnregisterFileFormatTools() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Tools").menu("Unregister File Format");
            fileMenuItem.click();

            SWTBotShell botshell = bot.shell("Unregister a file format");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Unregister a file format"));

            botshell.bot().comboBox().setSelection("FITS");
            botshell.bot().button("   &OK   ").click();

            fileMenuItem = bot.menu("Help").menu("Supported File Formats");
            fileMenuItem.click();

            botshell = bot.shell("Supported File Formats");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Supported File Formats"));
                    //("\\sSupported File Formats: \\s.*HDF5\\s.*NetCDF\\s.*HDF4\\s\\s");
            botshell.bot().button("   &OK   ").click();
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
    public void verifyUserOptionsDialog() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Tools").menu("User Options");
            fileMenuItem.click();

            SWTBotShell botshell = bot.shell("Preferences");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("Preferences"));

            SWTBotRadio rwButton = botshell.bot().radio("Read/Write");
            assertTrue(rwButton.isEnabled());

            //botshell.bot().button("Restore Defaults").click();

            botshell.bot().button("Cancel").click();
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
}
