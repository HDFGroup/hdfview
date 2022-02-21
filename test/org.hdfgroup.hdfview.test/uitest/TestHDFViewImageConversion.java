package uitest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
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
            bot.menu().menu("Tools").menu("Convert Image To").menu("HDF4").click();

            SWTBotShell convertshell = bot.shell("Convert Image to HDF4 ...");
            convertshell.activate();
            bot.waitUntil(Conditions.shellIsActive(convertshell.getText()));

            convertshell.bot().text(0).setText(workDir + File.separator + JPGFILE);

            String val = convertshell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("convertImageToHDF4()", "wrong source file", workDir + File.separator + JPGFILE, val),
                    val.equals(workDir + File.separator + JPGFILE));

            convertshell.bot().text(1).setText(workDir + File.separator + HDF4IMAGE);

            val = convertshell.bot().text(1).getText();
            assertTrue(constructWrongValueMessage("convertImageToHDF4()", "wrong dest file", workDir + File.separator + HDF4IMAGE, val),
                    val.equals(workDir + File.separator + HDF4IMAGE));

            convertshell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(convertshell));

            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue("convertImageToHDF4() filetree is missing file '" + HDF4IMAGE + "'",
                    items[0].getText().compareTo(HDF4IMAGE) == 0);
            assertTrue("convertImageToHDF4() filetree is missing image '" + JPGFILE + "'",
                    items[0].getNode(0).getText().compareTo(JPGFILE) == 0);

            items[0].getNode(0).click();

            // Test metadata

            SWTBotTabItem tabItem = bot.tabItem("General Object Info");
            tabItem.activate();

            val = bot.textWithLabel("Name: ").getText();
            assertTrue(constructWrongValueMessage("convertImageToHDF4()", "wrong image name", JPGFILE, val),
                    val.equals(JPGFILE));       // Test dataset name

            val = bot.textInGroup("Dataset Dataspace and Datatype", 0).getText();
            assertTrue(constructWrongValueMessage("convertImageToHDF4()", "wrong image rank", "2", val),
                    val.equals("2"));           // Test rank

            val = bot.textInGroup("Dataset Dataspace and Datatype", 1).getText();
            assertTrue(constructWrongValueMessage("convertImageToHDF4()", "wrong image dimension sizes", "533 x 533", val),
                    val.equals("533 x 533"));   // Test dimension sizes

            // Test sample pixels
            items[0].getNode(0).contextMenu().contextMenu("Open As").click();

            SWTBotShell openAsShell = bot.shell("Dataset Selection - /" + JPGFILE);
            openAsShell.bot().radio("&Image").click();
            openAsShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(openAsShell));

            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            SWTBotShell imageShell = bot.shells()[1];
            imageShell.activate();
            bot.waitUntil(Conditions.shellIsActive(imageShell.getText()));

            testSamplePixel(325, 53, "x=325,   y=53,   value=(152, 106, 91)");
            testSamplePixel(430, 357, "x=430,   y=357,   value=(83, 80, 107)");
            testSamplePixel(197, 239, "x=197,   y=239,   value=(206, 177, 159)");

            bot.activeShell().bot().menu().menu("Image").menu("Close").click();
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
    public void convertImageToHDF5() {
        File hdf_file = new File(workDir, HDF5IMAGE);

        try {
            bot.menu().menu("Tools").menu("Convert Image To").menu("HDF5").click();

            SWTBotShell convertshell = bot.shell("Convert Image to HDF5 ...");
            convertshell.activate();
            bot.waitUntil(Conditions.shellIsActive(convertshell.getText()));

            convertshell.bot().text(0).setText(workDir + File.separator + JPGFILE);

            String val = convertshell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("convertImageToHDF5()", "wrong source file", workDir + File.separator + JPGFILE, val),
                    val.equals(workDir + File.separator + JPGFILE));

            convertshell.bot().text(1).setText(workDir + File.separator + HDF5IMAGE);

            val = convertshell.bot().text(1).getText();
            assertTrue(constructWrongValueMessage("convertImageToHDF5()", "wrong dest file", workDir + File.separator + HDF5IMAGE, val),
                    val.equals(workDir + File.separator + HDF5IMAGE));

            convertshell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(convertshell));

            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue("convertImageToHDF5() filetree is missing file '" + HDF5IMAGE + "'",
                    items[0].getText().compareTo(HDF5IMAGE) == 0);
            assertTrue("convertImageToHDF5() filetree is missing image '" + JPGFILE + "'",
                    items[0].getNode(0).getText().compareTo(JPGFILE) == 0);

            // Test metadata
            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("General Object Info");
            tabItem.activate();

            val = bot.textWithLabel("Name: ").getText();
            assertTrue(constructWrongValueMessage("convertImageToHDF5()", "wrong image name", JPGFILE, val),
                    val.equals(JPGFILE));           // Test dataset name

            val = bot.textInGroup("Dataset Dataspace and Datatype", 0).getText();
            assertTrue(constructWrongValueMessage("convertImageToHDF5()", "wrong image rank", "3", val),
                    val.equals("3"));               // Test rank

            val = bot.textInGroup("Dataset Dataspace and Datatype", 1).getText();
            assertTrue(constructWrongValueMessage("convertImageToHDF5()", "wrong image dimension sizes", "533 x 533 x 3", val),
                    val.equals("533 x 533 x 3"));   // Test dimension sizes

            // Test sample pixels
            items[0].getNode(0).contextMenu().contextMenu("Open As").click();

            SWTBotShell openAsShell = bot.shell("Dataset Selection - /" + JPGFILE);
            openAsShell.bot().radio("&Image").click();
            openAsShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(openAsShell));

            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            SWTBotShell imageShell = bot.shells()[1];
            imageShell.activate();
            bot.waitUntil(Conditions.shellIsActive(imageShell.getText()));

            testSamplePixel(325, 53, "x=325,   y=53,   value=(152, 106, 91)");
            testSamplePixel(430, 357, "x=430,   y=357,   value=(83, 80, 107)");
            testSamplePixel(197, 239, "x=197,   y=239,   value=(206, 177, 159)");

            bot.activeShell().bot().menu().menu("Image").menu("Close").click();
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
}
