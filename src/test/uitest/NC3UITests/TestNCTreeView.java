package test.uitest.NC3UITests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;

import test.uitest.AbstractWindowTest;

public class TestNCTreeView extends AbstractWindowTest {

    @Test
    public void testclwvi_attributes() {
        String[][] expectedData =
            {   { "" },
                { "" } };
        SWTBotShell tableShell = null;
        String filename = "clwvi_test.nc";
        String groupname = "/";
        String datasetName = "";
        File hdfFile = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "testclwvi_attributes()", 2, filename);

            // Test metadata
            SWTBotTabItem tabItem = openMetadataTab(filetree, filename, groupname, "General Object Info");
            tabItem.activate();

            String val = bot.textWithLabel("Name: ").getText();
            assertTrue(constructWrongValueMessage("testclwvi_attributes()", "wrong name", groupname, val), val.equals(groupname)); // Test group name
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
            closeShell(tableShell);

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    @Test
    public void testRoy() {
        SWTBotShell tableShell = null;
        String filename = "Roy.nc";
        String datasetName = "/";
        File hdfFile = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "testRoy()", 3, filename);

            // Test metadata
            SWTBotTabItem tabItem = openMetadataTab(filetree, filename, datasetName, "General Object Info");
            tabItem.activate();

            String val = bot.textWithLabel("Name: ").getText();
            assertTrue(constructWrongValueMessage("testRoy()", "wrong name", datasetName, val), val.equals(datasetName)); // Test dataset name
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
            closeShell(tableShell);

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
