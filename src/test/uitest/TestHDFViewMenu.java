package test.uitest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

import hdf.HDFVersions;

//@RunWith(SWTBotJunit4ClassRunner.class)
public class TestHDFViewMenu extends AbstractWindowTest {

    private File createFile(String name, boolean hdf4_type) {
        String file_ext;
        String file_type;
        if (hdf4_type) {
            file_ext = new String(".hdf");
            file_type = new String("HDF4");
        }
        else {
            file_ext = new String(".h5");
            file_type = new String("HDF5");
        }

        File hdf_file = new File(workDir, name + file_ext);
        if (hdf_file.exists())
            hdf_file.delete();

        try {
            SWTBotMenu fileMenuItem = bot.menu("File").menu("New").menu(file_type);
            fileMenuItem.click();
            bot.sleep(500);

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText(name + file_ext);
//            fileChooser.approve();
//            bot.sleep(500);
//
//            assertTrue("File- " + hdf_file + " file created", hdf_file.exists());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }

        return hdf_file;
    }

    private File createHDF4File(String name) {
        return createFile(name, true);
    }

    private File createHDF5File(String name) {
        return createFile(name, false);
    }

    private void closeFile(File hdf_file, boolean delete_file) {
        try {
            SWTBotMenu fileMenuItem = bot.menu("File").menu("Close All");
            fileMenuItem.click();
            bot.sleep(500);

            if(delete_file) {
                assertTrue("closeFile File " + hdf_file + " not deleted", hdf_file.delete());
                assertFalse("closeFile File " + hdf_file + " not gone", hdf_file.exists());
            }

            SWTBotTree filetree = bot.tree();
            //filetree.setFocus();
            assertTrue("closeHDFFile filetree shows:"+filetree.rowCount(), filetree.rowCount() == 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

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

    @Ignore
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

    @Ignore
    public void verifyTextInLabelWhenClickingHDF4Button() {
        try {
            bot.toolbarButtonWithTooltip("HDF4 Library Version").click();

            SWTBotShell botshell = bot.shell("HDF Library Version");
            botshell.activate();
            bot.waitUntil(Conditions.shellIsActive("HDF Library Version"));

            assertEquals(bot.label().getText(), HDF4VERSION);
            bot.button("   &Ok   ").click();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Ignore
    public void verifyTextInLabelWhenClickingHDF5Button() {
        try {
            bot.toolbarButtonWithTooltip("HDF5 Library Version").click();
            bot.sleep(500);

            SWTBotShell shell = bot.shell("HDF5 Library Version");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive("HDF5 Library Version"));

            assertEquals(bot.label().getText(), HDF5VERSION);
            bot.button("   &Ok   ").click();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Ignore
    public void verifyButtonOpen() {
        File hdf_file = createHDF4File("testopenbutton");

        try {
            closeFile(hdf_file, false);

            bot.toolbarButton("Open").click();
            bot.sleep(500);

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testopenbutton.hdf");
//            fileChooser.approve();
//            bot.sleep(500);

            SWTBotTree filetree = bot.tree();
            assertTrue("Button-Open-HDF4 filetree shows: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("Button-Open-HDF4 filetree has file "+filetree.cell(0,0), (filetree.cell(0,0)).compareTo("testopenbutton.hdf") == 0);
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
    public void verifyButtonClose() {
        File hdf_file = createHDF4File("closebutton");

        try {
            SWTBotTree filetree = bot.tree();
            assertTrue("Button-Close-HDF4 filetree shows: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("Button-Close-HDF4 filetree has file "+filetree.cell(0,0), (filetree.cell(0,0)).compareTo("closebutton.hdf") == 0);

            filetree.select(0);
            bot.toolbarButton("Close").click();
            bot.sleep(500);

            assertTrue("Button-Close-HDF4 file deleted", hdf_file.delete());
            assertFalse("Button-Close-HDF4 file gone", hdf_file.exists());
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
//            bot.sleep(500);

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
            bot.sleep(500);

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testopenrofile.h5");
//            fileChooser.approve();
//            bot.sleep(500);

            SWTBotTree filetree = bot.tree();
            assertTrue("File-OpenRO-HDF5 filetree shows: "+filetree.rowCount(), filetree.rowCount() == 1);
            assertTrue("File-OpenRO-HDF5 filetree has file "+filetree.cell(0,0), (filetree.cell(0,0)).compareTo("testopenrofile.h5") == 0);

            filetree.select(0);
            SWTBotMenu deleteMenuItem = filetree.contextMenu("Delete");
            bot.sleep(500);
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
            bot.sleep(500);

            hdf_file = new File(workDir, "testfile.hdf");
            if (hdf_file.exists())
                hdf_file.delete();

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testfile.hdf");
//            fileChooser.approve();
//            bot.sleep(500);

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
            bot.sleep(500);

            hdf_file = new File(workDir, "testfile.h5");
            if (hdf_file.exists())
                hdf_file.delete();

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testfile.h5");
//            fileChooser.approve();
//            bot.sleep(500);

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
            bot.sleep(500);

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
            bot.sleep(500);

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
            bot.sleep(500);

            bot.text("groupname").setText("grouptestname");
            bot.button("OK").click();
            bot.sleep(500);

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Save");
            fileMenuItem.click();
            bot.sleep(500);

            closeFile(hdf_file, false);

            fileMenuItem = bot.menu("File").menu("Open");
            fileMenuItem.click();
            bot.sleep(500);
//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testsavefile.h5");
//            fileChooser.approve();
//            bot.sleep(500);

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
            bot.sleep(500);

            bot.text("groupname").setText("grouptestname");
            bot.button("OK").click();
            bot.sleep(500);

            SWTBotMenu fileMenuItem = bot.menu("File").menu("Save As");
            fileMenuItem.click();
            bot.sleep(500);

//            JFileChooserFixture fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testsaveasfile2.h5");
//            fileChooser.approve();
//            bot.sleep(500);

            closeFile(hdf_file, true);

            SWTBotMenu fileOpenMenuItem = bot.menu("File").menu("Open");
            fileOpenMenuItem.click();
            bot.sleep(500);

//            fileChooser = JFileChooserFinder.findFileChooser().using(bot.robot);
//            fileChooser.fileNameTextBox().setText("testsaveasfile2.h5");
//            fileChooser.approve();
//            bot.sleep(500);

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
    public void verifyTextInLabelWhenClickingHDF4Help() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Help").menu("HDF4 Library Version");
            fileMenuItem.click();
            bot.sleep(500);

                    //(HDF4VERSION);
            bot.button("   &Ok   ").click();
            bot.sleep(500);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Ignore
    public void verifyTextInLabelWhenClickingHDF5Help() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Help").menu("HDF5 Library Version");
            fileMenuItem.click();
            bot.sleep(500);

                    //(HDF5VERSION);
            bot.button("   &Ok   ").click();
            bot.sleep(500);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Ignore
    public void verifyTextInLabelWhenClickingJavaHelp() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Help").menu("Java Version");
            fileMenuItem.click();
            bot.sleep(500);

                    //("Compiled at jdk 1.7.*\\sRunning at.*");
            bot.button("   &Ok   ").click();
            bot.sleep(500);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Ignore
    public void verifyTextInLabelWhenClickingAboutHelp() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Help").menu("About...");
            fileMenuItem.click();
            bot.sleep(500);

            SWTBotShell shell = bot.shell("About HDFView");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive("About HDFView"));

                    //("HDF Viewer, Version " + VERSION + "\\sFor.*\\s\\sCopyright.*2006-2015 The HDF Group.\\sAll rights reserved.");
            bot.button("   &Ok   ").click();
            bot.sleep(500);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Ignore
    public void verifyTextInLabelWhenClickingSupportedFileFormatsHelp() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Help").menu("Supported File Formats");
            fileMenuItem.click();
            bot.sleep(500);

            SWTBotShell shell = bot.shell("Supported File Formats");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive("Supported File Formats"));
                    //("\\sSupported File Formats: \\s.*Fits\\s.*HDF5\\s.*NetCDF\\s.*HDF4\\s\\s");
            bot.button("   &Ok   ").click();
            bot.sleep(500);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Ignore
    public void verifyRegisterFileFormatTools() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Tools").menu("Unregister File Format");
            fileMenuItem.click();
            bot.sleep(500);
            SWTBotShell shell = bot.shell("Unregister File Format");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive("Unregister File Format"));

            bot.comboBox().setSelection("Fits");
            bot.button("   &Ok   ").click();
            bot.sleep(500);

            fileMenuItem = bot.menu("Help").menu("Supported File Formats");
            fileMenuItem.click();
            bot.sleep(500);

            shell = bot.shell("Supported File Formats");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive("Supported File Formats"));
                    //("\\sSupported File Formats: \\s.*HDF5\\s.*NetCDF\\s.*HDF4\\s\\s"));
            bot.button("   &OK   ").click();
            bot.sleep(500);

            fileMenuItem = bot.menu("Tools").menu("Register File Format");
            fileMenuItem.click();
            bot.sleep(500);
            shell= bot.shell("Register a file format");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive("Register a file format"));

            bot.text().setText("Fits:hdf.object.fits.FitsFile:fits");
            bot.sleep(500);
            bot.button("   &OK   ").click();
            bot.sleep(500);

            fileMenuItem = bot.menu("Help").menu("Supported File Formats");
            fileMenuItem.click();
            bot.sleep(500);

            shell = bot.shell("Supported File Formats");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive("Supported File Formats"));
                    //("\\sSupported File Formats: \\s.*Fits\\s.*HDF5\\s.*NetCDF\\s.*HDF4\\s\\s");
            bot.button("   &OK   ").click();
            bot.sleep(500);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }

    @Ignore
    public void verifyUnregisterFileFormatTools() {
        try {
            SWTBotMenu fileMenuItem = bot.menu("Tools").menu("Unregister File Format");
            fileMenuItem.click();
            bot.sleep(500);
            SWTBotShell shell = bot.shell("Unregister a file format");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive("Unregister a file format"));

            bot.comboBox().setSelection("Fits");
            bot.sleep(500);
            bot.button("   &OK   ").click();
            bot.sleep(500);

            fileMenuItem = bot.menu("Help").menu("Supported File Formats");
            fileMenuItem.click();
            bot.sleep(500);

            shell = bot.shell("Supported File Formats");
            shell.activate();
            bot.waitUntil(Conditions.shellIsActive("Supported File Formats"));
                    //("\\sSupported File Formats: \\s.*HDF5\\s.*NetCDF\\s.*HDF4\\s\\s");
            bot.button("   &OK   ").click();
            bot.sleep(500);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
        }
    }
}
