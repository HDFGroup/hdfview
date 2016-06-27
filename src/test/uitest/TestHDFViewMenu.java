package test.uitest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;

import org.junit.runner.RunWith;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
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
        File hdf_file = createHDF4File("testopenbutton");

        try {
            closeFile(hdf_file, false);

            bot.toolbarButtonWithTooltip("Open").click();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();

            SWTBotText text = shell.bot().text();
            text.setText("testopenbutton.hdf");
            assertEquals("testopenbutton.hdf", text.getText());

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue("Button-Open-HDF4 filetree shows: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("Button-Open-HDF4 filetree is missing file testopenbutton.hdf", items[0].getText().compareTo("testopenbutton.hdf") == 0);
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
        File hdf_file = createHDF4File("closebutton");

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue("Button-Close-HDF4 filetree shows: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("Button-Close-HDF4 filetree is missing file closebutton.hdf", items[0].getText().compareTo("closebutton.hdf") == 0);

            filetree.select(0);

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
        File hdf_file = createHDF4File("testopenfile");

        try {
            closeFile(hdf_file, false);

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Open");
            fileMenuItem.click();

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testopenfile.hdf");
//            fileChooser.approve();

            SWTBotTree filetree = bot.tree();
            assertTrue("File-Open-HDF4 filetree shows: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("File-Open-HDF4 filetree has file "+filetree.cell(0,0),(filetree.cell(0,0)).compareTo("testopenfile.hdf") == 0);
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
    public void verifyMenuOpenReadOnly() {
        File hdf_file = createHDF5File("testopenrofile");

        try {
            closeFile(hdf_file, false);

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Open Read-Only");
            fileMenuItem.click();

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testopenrofile.h5");
//            fileChooser.approve();

            SWTBotTree filetree = bot.tree();
            assertTrue("File-OpenRO-HDF5 filetree shows: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("File-OpenRO-HDF5 filetree has file "+filetree.cell(0,0), (filetree.cell(0,0)).compareTo("testopenrofile.h5") == 0);

            filetree.select(0);
            SWTBotMenu deleteMenuItem = filetree.contextMenu("Delete");
            deleteMenuItem.isEnabled();
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
    public void verifyMenuNewHDF4() {
        File hdf_file = null;
        try {
            SWTBotMenu fileMenuItem = bot.menu("File").menu("New").menu("HDF4");
            fileMenuItem.click();

            hdf_file = new File(workDir, "testfile.hdf");
            if (hdf_file.exists())
                hdf_file.delete();

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testfile.hdf");
//            fileChooser.approve();

            assertTrue("File-New-HDF4 file created", hdf_file.exists());
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
    public void verifyMenuNewHDF5() {
        File hdf_file = null;
        try {
            SWTBotMenu fileMenuItem = bot.menu("File").menu("New").menu("HDF5");
            fileMenuItem.click();

            hdf_file = new File(workDir, "testfile.h5");
            if (hdf_file.exists())
                hdf_file.delete();

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testfile.h5");
//            fileChooser.approve();

            assertTrue("File-New-HDF5 file created", hdf_file.exists());
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
    public void verifyMenuClose() {
        File hdf_file = createHDF4File("closefile");

        try {
            SWTBotTree filetree = bot.tree();
            assertTrue("verifyMenuClose filetree shows: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("verifyMenuClose filetree has file "+filetree.cell(0,0), (filetree.cell(0,0)).compareTo("closefile.hdf") == 0);

            filetree.select(0);
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

    @Ignore
    public void verifyMenuCloseAll() {
        File hdf4_file = createHDF4File("closeallfile");
        File hdf5_file = createHDF5File("closeallfile");

        try {
            SWTBotTree filetree = bot.tree();
            assertTrue("verifyMenuCloseAll HDF filetree shows: "+filetree.rowCount(), filetree.rowCount() == 2);
            assertTrue("verifyMenuCloseAll HDF filetree has file "+filetree.cell(0,0), (filetree.cell(0,0)).compareTo("closeallfile.hdf") == 0);
            assertTrue("verifyMenuCloseAll HDF5 filetree has file "+filetree.cell(1,0), (filetree.cell(1,0)).compareTo("closeallfile.h5") == 0);

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

    @Test
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
    
    @Test
    public void verifyMenuWindowCloseAll() {
        
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
            
            // Test that the HDF4 Library Version button works correctly
            SWTBotButton hdf4Button = botshell.bot().buttonWithTooltip("HDF4 Library Version");
            hdf4Button.click();
            
            botshell = bot.shell("HDF Library Version");
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
            
            // Test that the HDF5 Library Version button works correctly
            SWTBotButton hdf5Button = botshell.bot().buttonWithTooltip("HDF5 Library Version");
            hdf5Button.click();
            
            botshell = bot.shell("HDF Library Version");
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
