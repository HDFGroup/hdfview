package test.uitest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLabel;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

import hdf.HDFVersions;

public class TestHDFViewLibBounds extends AbstractWindowTest {
    // the version of the HDFViewer
    private static String VERSION = HDFVersions.HDFVIEW_VERSION;
    private static String workDir = System.getProperty("hdfview.workdir");
    
    @Test
    public void testLibVersion() {
        File hdf_file = createHDF5File("test_libversion");
        
        try {
            closeFile(hdf_file, false);
            
            bot.toolbarButtonWithTooltip("Open").click();
            
            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();
            
            SWTBotText text = shell.bot().text();
            text.setText("test_libversion.h5");
            assertEquals("test_libversion.h5", text.getText());
            
            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));
            
            final SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            
            assertTrue("Button-Open-HDF5 filetree shows:", filetree.rowCount() == 1);
            assertTrue("Button-Open-HDF5 filetree has file test_libversion.h5", items[0].getText().compareTo("test_libversion.h5") == 0);
            
            filetree.select(0).contextMenu("Set Lib version bounds").click();
            
            // Set selected object
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    Tree tree = filetree.widget;
                    TreeItem item = filetree.getAllItems()[0].widget;
                    
                    Event event = new Event();
                    event.x = item.getBounds().x;
                    event.y = item.getBounds().y;
                    
                    tree.notifyListeners(SWT.MouseUp, event);
                }
            });
            
            SWTBotShell libVersionShell = bot.shell("Set the library version bounds: ");
            libVersionShell.activate();
            
            libVersionShell.bot().comboBox(0).setSelection("Earliest");
            
            libVersionShell.bot().button("   &OK   ").click();
            
            bot.waitUntil(shellCloses(libVersionShell));
            
            // filetree.select(0).contextMenu("Show Properties").click();
            
            // Workaround to show Properties, since selected object doesn't get set
            filetree.contextMenu("Show Properties").click();
            
            SWTBotShell propertiesWindow = bot.shell("Properties - /");
            propertiesWindow.activate();
            
            assertEquals("Earliest and Latest", propertiesWindow.bot().label(7).getText());
            
            propertiesWindow.bot().button("   &Close   ").click();
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

//@Test
//public void testLibVersion() {
//    try {
//        mainFrameFixture.dialog().label("libverbound").requireText();
//
//        mainFrameFixture.dialog().button("Close").click();
//        mainFrameFixture.robot.waitForIdle();
//
//        filetree.showPopupMenuAt(0).menuItemWithPath("Set Lib version bounds").click();
//        mainFrameFixture.robot.waitForIdle();
//
//        // Test Latest and Latest
//        mainFrameFixture.dialog().comboBox("earliestversion").selectItem("Latest");
//        mainFrameFixture.robot.waitForIdle();
//
//        mainFrameFixture.dialog().optionPane().component().setValue("Ok");
//        mainFrameFixture.robot.waitForIdle();
//
//        filetree.showPopupMenuAt(0).menuItemWithPath("Show Properties").click();
//        mainFrameFixture.robot.waitForIdle();
//
//        mainFrameFixture.dialog().label("libverbound").requireText("Latest and Latest");
//
//        mainFrameFixture.dialog().button("Close").click();
//        mainFrameFixture.robot.waitForIdle();
//    }
//    catch (Exception ex) {
//        ex.printStackTrace();
//    }
//    catch(AssertionError ae) {
//        ae.printStackTrace();
//    }
//    
//}