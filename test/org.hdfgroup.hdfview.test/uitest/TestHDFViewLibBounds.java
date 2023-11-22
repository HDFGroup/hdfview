package uitest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;

import java.io.File;

import org.junit.Test;

import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class TestHDFViewLibBounds extends AbstractWindowTest {
    @Test
    public void testLibVersion()
    {
        String testFilename = "test_libversion.h5";
        File hdfFile        = createFile(testFilename);

        try {
            closeFile(hdfFile, false);
            hdfFile = openFile(testFilename, FILE_MODE.READ_WRITE);

            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testLibVersion()", 1, testFilename);

            SWTBotTabItem tabItem = openMetadataTab(filetree, testFilename, "/", "General Object Info");
            tabItem.activate();

            String val = bot.textWithLabel("Library version bounds: ").getText();
            assertTrue(
                constructWrongValueMessage("testLibVersion()", "wrong lib bounds", "Earliest and V116", val),
                val.equals("Earliest and V116"));

            SWTBotTreeItem[] items = filetree.getAllItems();
            items[0].click();
            items[0].contextMenu().contextMenu("Set Lib version bounds").click();

            SWTBotShell libVersionShell = bot.shell("Set the library version bounds: ");
            libVersionShell.activate();

            libVersionShell.bot().comboBox(0).setSelection("V18");

            libVersionShell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(libVersionShell));

            val = bot.textWithLabel("Library version bounds: ").getText();
            assertTrue(
                constructWrongValueMessage("testLibVersion()", "wrong lib bounds", "V18 and V116", val),
                val.equals("V18 and V116"));

            items[0].contextMenu().contextMenu("Set Lib version bounds").click();

            libVersionShell = bot.shell("Set the library version bounds: ");
            libVersionShell.activate();

            libVersionShell.bot().comboBox(1).setSelection("V18");

            libVersionShell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(libVersionShell));

            val = bot.textWithLabel("Library version bounds: ").getText();
            assertTrue(constructWrongValueMessage("testLibVersion()", "wrong lib bounds", "V18 and V18", val),
                       val.equals("V18 and V18"));

            items[0].contextMenu().contextMenu("Set Lib version bounds").click();

            libVersionShell = bot.shell("Set the library version bounds: ");
            libVersionShell.activate();

            libVersionShell.bot().comboBox(0).setSelection("Latest");

            libVersionShell.bot().button("   &OK   ").click();
            SWTBotShell libVersionErrorShell = bot.shells()[2];
            libVersionErrorShell.activate();
            libVersionErrorShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(libVersionErrorShell));

            libVersionShell.bot().button(" &Cancel ").click();
            bot.waitUntil(shellCloses(libVersionShell));
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
                closeFile(hdfFile, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
