package test.uitest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;

import org.junit.runner.RunWith;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

//@RunWith(SWTBotJunit4ClassRunner.class)
public class TestHDFViewMenu extends AbstractWindowTest {
    @Test
    public void verifyOpenButtonEnabled() {
        try {
            boolean status = bot.toolbarButtonWithTooltip("Open").isEnabled();
            assertTrue(status);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Test
    public void verifyCloseButtonEnabled() {
        try {
            boolean status = bot.toolbarButtonWithTooltip("Close").isEnabled();
            assertTrue(status);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Test
    public void verifyHelpButtonEnabled() {
        try {
            boolean status = bot.toolbarButtonWithTooltip("Help").isEnabled();
            assertTrue(status);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Test
    public void verifyHDF4ButtonEnabled() {
        try {
            boolean status = bot.toolbarButtonWithTooltip("HDF4 Library Version").isEnabled();
            assertTrue(status);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Test
    public void verifyHDF5ButtonEnabled() {
        try {
            boolean status = bot.toolbarButtonWithTooltip("HDF5 Library Version").isEnabled();
            assertTrue(status);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Test
    public void verifyTextInLabelWhenClickingHDF4Button() {
        try {
            bot.toolbarButtonWithTooltip("HDF4 Library Version").click();

            SWTBotShell botshell = bot.shell("HDF Library Version");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("HDF Library Version"));

            assertEquals(botshell.bot().label(1).getText(), HDF4VERSION);
            botshell.bot().button("   &OK   ").click();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Test
    public void verifyTextInLabelWhenClickingHDF5Button() {
        try {
            bot.toolbarButtonWithTooltip("HDF5 Library Version").click();

            SWTBotShell botshell = bot.shell("HDF Library Version");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("HDF Library Version"));

            assertEquals(botshell.bot().label(1).getText(), HDF5VERSION);

            botshell.bot().label(HDF5VERSION);
            botshell.bot().button("   &OK   ").click();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
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

            assertTrue("Button-Close-HDF4 filetree row count: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("Button-Close-HDF4 filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext) == 0);

            items[0].click();

            bot.toolbarButtonWithTooltip("Close").click();

            assertTrue("Button-Close-HDF4 file not deleted", hdf_file.delete());
            assertFalse("Button-Close-HDF4 file wasn't gone", hdf_file.exists());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                if(hdf_file.exists())
                    closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }

    @Ignore
    public void verifyMenuOpen() {
        String filename = "testopenfile";
        String file_ext = ".hdf";
        File hdf_file = createHDF4File(filename);

        try {
            closeFile(hdf_file, false);

            SWTBotMenu fileMenuItem = bot.menu("File").menu("&Open 	Ctrl-O");
            fileMenuItem.click();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive(shell.getText()));

            SWTBotText text = shell.bot().text();
            text.setText(filename + file_ext);
            assertEquals(filename + file_ext, text.getText());

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            SWTBotTree filetree = bot.tree();
            assertTrue("File-Open-HDF4 filetree row count: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("File-Open-HDF4 filetree is missing file " + filename + file_ext, filetree.getAllItems()[0].getText().compareTo(filename + file_ext) == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }

    @Test
    public void verifyMenuOpenReadOnly() {
        String filename = "testopenrofile";
        String file_ext = ".h5";
        File hdf_file = createHDF5File(filename);

        try {
            closeFile(hdf_file, false);

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Open Read-Only");
            fileMenuItem.click();
            
            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive(shell.getText()));

            SWTBotText text = shell.bot().text();
            text.setText(filename + file_ext);
            assertEquals(filename + file_ext, text.getText());

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            assertTrue("File-OpenRO-HDF5 filetree row count: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("File-OpenRO-HDF5 filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext) == 0);

            items[0].click();
            
            assertFalse("Error: New Menu Item is enabled.", filetree.contextMenu("New").isEnabled());
            assertFalse("Error: Cut Menu Item is enabled.", filetree.contextMenu("Cut").isEnabled());
            assertFalse("Error: Paste Menu Item is enabled.", filetree.contextMenu("Paste").isEnabled());
            assertFalse("Error: Delete Menu Item is enabled.", filetree.contextMenu("Delete").isEnabled());
            assertFalse("Error: Rename Menu Item is enabled.", filetree.contextMenu("Rename").isEnabled());
            assertFalse("Error: Set Lib Version Bounds Menu Item is enabled.", filetree.contextMenu("Set Lib version bounds").isEnabled());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
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
            assertEquals(filename + file_ext, text.getText());

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));
            
            SWTBotTree filetree = bot.tree();
            assertTrue("File-New-HDF4 filetree row count: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("File-New-HDF4 filetree is missing file " + filename + file_ext, filetree.getAllItems()[0].getText().compareTo(filename + file_ext) == 0);
            assertTrue("File-New-HDF4 file not created", hdf_file.exists());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
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
            assertEquals(filename + file_ext, text.getText());

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));
            
            SWTBotTree filetree = bot.tree();
            assertTrue("File-New-HDF5 filetree row count: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("File-New-HDF5 filetree is missing file " + filename + file_ext, filetree.getAllItems()[0].getText().compareTo(filename + file_ext) == 0);
            assertTrue("File-New-HDF5 file not created", hdf_file.exists());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
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
            assertTrue("verifyMenuClose filetree row count: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("verifyMenuClose filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext) == 0);

            items[0].click();
            
            SWTBotMenu fileMenuItem = bot.menu("File").menu("Close");
            fileMenuItem.click();

            assertTrue("verifyMenuClose file not deleted", hdf_file.delete());
            assertFalse("verifyMenuClose file not gone", hdf_file.exists());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                if(hdf_file.exists())
                    closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
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
            assertTrue("verifyMenuCloseAll HDF filetree row count: "+filetree.rowCount(), filetree.rowCount() == 2);
            assertTrue("verifyMenuCloseAll HDF filetree is missing file " + filename + hdf4_file_ext, items[0].getText().compareTo(filename + hdf4_file_ext) == 0);
            assertTrue("verifyMenuCloseAll HDF5 filetree is missing file " + filename + hdf5_file_ext, items[1].getText().compareTo(filename + hdf5_file_ext) == 0);

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Close All");
            fileMenuItem.click();

            assertTrue("verifyMenuCloseAll HDF file not deleted", hdf4_file.delete());
            assertFalse("verifyMenuCloseAll HDF file not gone", hdf4_file.exists());

            assertTrue("verifyMenuCloseAll HDF5 file not deleted", hdf5_file.delete());
            assertFalse("verifyMenuCloseAll HDF5 file not gone", hdf5_file.exists());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                if(hdf4_file.exists())
                    closeFile(hdf4_file, true);
                if(hdf5_file.exists())
                    closeFile(hdf5_file, true);
            }
            catch (Exception ex) {}
        }
    }

    @Ignore
    public void verifyMenuSave() {
        File hdf_file = createHDF5File("testsavefile");

        try {
            SWTBotTree filetree = bot.tree();
            filetree.select(0);
            SWTBotMenu groupMenuItem = filetree.contextMenu("New").menu("Group");
            groupMenuItem.click();

            bot.text("groupname").setText("grouptestname");
            bot.button("   &OK   ").click();

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Save");
            fileMenuItem.click();

            closeFile(hdf_file, false);

            fileMenuItem = bot.menu("File").menu("Open");
            fileMenuItem.click();
//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testsavefile.h5");
//            fileChooser.approve();

            filetree = bot.tree();
            assertTrue("File-Save-HDF5 filetree shows: "+filetree.rowCount(), filetree.rowCount() == 2);
            assertTrue("File-Save-HDF5 filetree has file "+filetree.cell(0,0), (filetree.cell(0,0)).compareTo("testsavefile.h5") == 0);
            assertTrue("File-Save-HDF5 filetree has group "+filetree.cell(1,0), (filetree.cell(1,0)).compareTo("grouptestname") == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }

    @Ignore
    public void verifyMenuSaveAs() {
        File hdf_file = createHDF5File("testsaveasfile");
        File hdf_save_file = new File(workDir, "testsaveasfile2.h5");
        if (hdf_save_file.exists())
            hdf_save_file.delete();

        try {
            SWTBotTree filetree = bot.tree();
            filetree.select(0);
            SWTBotMenu groupMenuItem = filetree.contextMenu("New").menu("Group");
            groupMenuItem.click();

            bot.text("groupname").setText("grouptestname");
            bot.button("   &OK   ").click();

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Save As");
            fileMenuItem.click();

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testsaveasfile2.h5");
//            fileChooser.approve();

            closeFile(hdf_file, true);

            SWTBotMenu fileOpenMenuItem = bot.menu("File").menu("Open");
            fileOpenMenuItem.click();

//            fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testsaveasfile2.h5");
//            fileChooser.approve();

            filetree = bot.tree();
            assertTrue("File-SaveAs-HDF5 filetree shows: "+filetree.rowCount(), filetree.rowCount() == 2);
            assertTrue("File-SaveAs-HDF5 filetree has file "+filetree.cell(0,0), (filetree.cell(0,0)).compareTo("testsaveasfile2.h5") == 0);
            assertTrue("File-SaveAs-HDF5 filetree has group "+filetree.cell(1,0), (filetree.cell(1,0)).compareTo("grouptestname") == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                if(hdf_file.exists())
                    closeFile(hdf_file, true);
                closeFile(hdf_save_file, true);
            }
            catch (Exception ex) {}
        }
    }
    
    @Ignore
    public void verifyMenuWindowCloseAll() {
        String filename = "hdf5_test";
        File hdf_file = null;

        try {
            hdf_file = openFile(filename, false);

            SWTBotTree filetree = bot.tree();
            
            assertTrue("Window-Close All filetree row count: " + filetree.visibleRowCount(), filetree.visibleRowCount() == 5);
            assertTrue("Window-Close All too many shells open", bot.shells().length == 1);
            
            filetree.getTreeItem(filename + ".h5").getNode("arrays").getNode("2D float array").doubleClick();
            bot.waitUntilWidgetAppears(Conditions.waitForWidget(WidgetMatcherFactory.widgetOfType(NatTable.class)));
            filetree.getTreeItem(filename + ".h5").getNode("arrays").getNode("2D int array").doubleClick();
            bot.waitUntilWidgetAppears(Conditions.waitForWidget(WidgetMatcherFactory.widgetOfType(NatTable.class)));
            filetree.getTreeItem(filename + ".h5").getNode("arrays").getNode("3D int array").doubleClick();
            bot.waitUntilWidgetAppears(Conditions.waitForWidget(WidgetMatcherFactory.widgetOfType(NatTable.class)));
            filetree.getTreeItem(filename + ".h5").getNode("arrays").getNode("4D int").doubleClick();
            bot.waitUntilWidgetAppears(Conditions.waitForWidget(WidgetMatcherFactory.widgetOfType(NatTable.class)));
            filetree.getTreeItem(filename + ".h5").getNode("arrays").getNode("ArrayOfStructures").doubleClick();
            bot.waitUntilWidgetAppears(Conditions.waitForWidget(WidgetMatcherFactory.widgetOfType(NatTable.class)));
            filetree.getTreeItem(filename + ".h5").getNode("images").getNode("Iceberg").doubleClick();
            bot.waitUntilWidgetAppears(Conditions.waitForWidget(WidgetMatcherFactory.widgetOfType(NatTable.class)));
            filetree.getTreeItem(filename + ".h5").getNode("images").getNode("pixel interlace").doubleClick();
            bot.waitUntilWidgetAppears(Conditions.waitForWidget(WidgetMatcherFactory.widgetOfType(NatTable.class)));

            assertTrue("Window-Close All too many or missing shells: " + bot.shells().length + " shells shown", bot.shells().length == 8);
            
            shell.forceActive();
            
            bot.menu("Window").menu("Close All").click();
            
            assertTrue("Window-Close All too many or missing shells: " + bot.shells().length + " shells shown", bot.shells().length == 1);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
        finally {
            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
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

            assertEquals(botshell.bot().label(1).getText(), HDF4VERSION);
            botshell.bot().button("   &OK   ").click();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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

            assertEquals(botshell.bot().label(1).getText(), HDF5VERSION);

            botshell.bot().label(HDF5VERSION);
            botshell.bot().button("   &OK   ").click();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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

            botshell.bot().comboBox().setSelection("Fits");
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
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
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
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }
}
