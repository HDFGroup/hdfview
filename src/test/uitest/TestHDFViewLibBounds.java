package test.uitest;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

public class TestHDFViewLibBounds extends AbstractWindowTest {
    @Test
    public void testLibVersion() {
        String filename = "test_libversion";
        String file_ext = ".h5";
        File hdf_file = createHDF5File(filename);

        try {
            closeFile(hdf_file, false);

            bot.toolbarButtonWithTooltip("Open").click();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();

            SWTBotText text = shell.bot().text();
            text.setText(filename + file_ext);

            String val = text.getText();
            assertTrue(constructWrongValueMessage("testLibVersion()", "wrong file name", filename + file_ext, val),
                    val.equals(filename + file_ext));

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            final SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testLibVersion()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("testLibVersion() filetree is missing file '" + filename + file_ext + "'", items[0].getText().compareTo(filename + file_ext) == 0);

            items[0].click();
            items[0].contextMenu("Set Lib version bounds").click();

            SWTBotShell libVersionShell = bot.shell("Set the library version bounds: ");
            libVersionShell.activate();

            libVersionShell.bot().comboBox(0).setSelection("Earliest");

            libVersionShell.bot().button("   &OK   ").click();

            bot.waitUntil(shellCloses(libVersionShell));

            items[0].click();
            items[0].contextMenu("Show Properties").click();

            SWTBotShell propertiesWindow = bot.shell("Properties - /");
            propertiesWindow.activate();

            val = propertiesWindow.bot().label(7).getText();
            assertTrue(constructWrongValueMessage("testLibVersion()", "wrong lib bounds", "Earliest and Latest", val),
                    val.equals("Earliest and Latest"));

            propertiesWindow.bot().button("   &Close   ").click();

            items[0].click();
            items[0].contextMenu("Set Lib version bounds").click();

            libVersionShell = bot.shell("Set the library version bounds: ");
            libVersionShell.activate();

            libVersionShell.bot().comboBox(0).setSelection("Latest");

            libVersionShell.bot().button("   &OK   ").click();

            bot.waitUntil(shellCloses(libVersionShell));

            items[0].click();
            items[0].contextMenu("Show Properties").click();

            propertiesWindow = bot.shell("Properties - /");
            propertiesWindow.activate();

            val = propertiesWindow.bot().label(7).getText();
            assertTrue(constructWrongValueMessage("testLibVersion()", "wrong lib bounds", "Latest and Latest", val),
                    val.equals("Latest and Latest"));

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
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}