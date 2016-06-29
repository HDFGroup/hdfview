package test.uitest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

public class TestHDFViewImageConversion extends AbstractWindowTest {
    private static String JPGFILE = "apollo17_earth.jpg";
    private static String HDF4IMAGE = JPGFILE + ".hdf";
    private static String HDF5IMAGE = JPGFILE + ".h5";
    
    @Test
    public void convertImageToHDF4() {
        File hdf_file = new File(workDir, HDF4IMAGE);

        try {
            bot.menu("Tools").menu("Convert Image To").menu("HDF4").click();

            SWTBotShell convertshell = bot.shell("Convert Image to HDF4 ...");
            convertshell.activate();
            bot.waitUntil(Conditions.shellIsActive(convertshell.getText()));

            convertshell.bot().text(0).setText(workDir + File.separator + JPGFILE);
            assertEquals(convertshell.bot().text(0).getText(), workDir + File.separator + JPGFILE);

            convertshell.bot().text(1).setText(workDir + File.separator + HDF4IMAGE);
            assertEquals(convertshell.bot().text(1).getText(), workDir + File.separator + HDF4IMAGE);

            convertshell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(convertshell));

            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue("convertImageToHDF4 filetree is missing file " + HDF4IMAGE,
                    items[0].getText().compareTo(HDF4IMAGE) == 0);
            assertTrue("convertImageToHDF4 filetree is missing image " + JPGFILE,
                    items[0].getNode(0).getText().compareTo(JPGFILE) == 0);

            items[0].getNode(0).click();

            // Test metadata
            filetree.contextMenu("Show Properties").click();

            SWTBotShell metaDataShell = bot.shell("Properties - /" + JPGFILE);
            metaDataShell.activate();
            bot.waitUntil(Conditions.shellIsActive(metaDataShell.getText()));

            assertEquals(metaDataShell.bot().label(1).getText(), JPGFILE);      // Test dataset name
            assertEquals(metaDataShell.bot().text(0).getText(), "2");           // Test rank
            assertEquals(metaDataShell.bot().text(1).getText(), "533 x 533");   // Test dimension sizes

            metaDataShell.bot().button("   &Close   ").click();
            bot.waitUntil(Conditions.shellCloses(metaDataShell));

            items[0].getNode(0).click();

            // Test sample pixels
            filetree.contextMenu("Open As").click();

            SWTBotShell openAsShell = bot.shell("Dataset Selection - /" + JPGFILE);
            openAsShell.bot().radio("&Image").click();
            openAsShell.bot().button("   &OK   ").click();

            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(".*at.*\\[.*in.*\\]")));

            testSamplePixel(325, 53, "x=325,   y=53,   value=(152, 106, 91)");
            testSamplePixel(430, 357, "x=430,   y=357,   value=(83, 80, 107)");
            testSamplePixel(197, 239, "x=197,   y=239,   value=(206, 177, 159)");
            
            bot.activeShell().bot().menu("Close").click();
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
    public void convertImageToHDF5() {
        File hdf_file = new File(workDir, HDF5IMAGE);

        try {
            bot.menu("Tools").menu("Convert Image To").menu("HDF5").click();

            SWTBotShell convertshell = bot.shell("Convert Image to HDF5 ...");
            convertshell.activate();
            bot.waitUntil(Conditions.shellIsActive(convertshell.getText()));

            convertshell.bot().text(0).setText(workDir + File.separator + JPGFILE);
            assertEquals(convertshell.bot().text(0).getText(), workDir + File.separator + JPGFILE);

            convertshell.bot().text(1).setText(workDir + File.separator + HDF5IMAGE);
            assertEquals(convertshell.bot().text(1).getText(), workDir + File.separator + HDF5IMAGE);

            convertshell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(convertshell));

            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue("convertImageToHDF5 filetree is missing file " + HDF5IMAGE,
                    items[0].getText().compareTo(HDF5IMAGE) == 0);
            assertTrue("convertImageToHDF5 filetree is missing image " + JPGFILE,
                    items[0].getNode(0).getText().compareTo(JPGFILE) == 0);

            items[0].getNode(0).click();

            // Test metadata
            filetree.contextMenu("Show Properties").click();

            SWTBotShell metaDataShell = bot.shell("Properties - /" + JPGFILE);
            metaDataShell.activate();
            bot.waitUntil(Conditions.shellIsActive(metaDataShell.getText()));

            assertEquals(metaDataShell.bot().label(1).getText(), JPGFILE);      // Test dataset name
            assertEquals(metaDataShell.bot().text(0).getText(), "3");           // Test rank
            assertEquals(metaDataShell.bot().text(1).getText(), "533 x 533 x 3");   // Test dimension sizes

            metaDataShell.bot().button("   &Close   ").click();
            bot.waitUntil(Conditions.shellCloses(metaDataShell));

            items[0].getNode(0).click();

            // Test sample pixels
            filetree.contextMenu("Open As").click();

            SWTBotShell openAsShell = bot.shell("Dataset Selection - /" + JPGFILE);
            openAsShell.bot().radio("&Image").click();
            openAsShell.bot().button("   &OK   ").click();

            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(".*at.*\\[.*in.*\\]")));

            testSamplePixel(325, 53, "x=325,   y=53,   value=(152, 106, 91)");
            testSamplePixel(430, 357, "x=430,   y=357,   value=(83, 80, 107)");
            testSamplePixel(197, 239, "x=197,   y=239,   value=(206, 177, 159)");
            
            bot.activeShell().bot().menu("Close").click();
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
}
