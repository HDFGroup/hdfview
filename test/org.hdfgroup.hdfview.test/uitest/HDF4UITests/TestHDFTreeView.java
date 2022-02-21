package uitest.HDF4UITests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;

import uitest.AbstractWindowTest;
import uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

public class TestHDFTreeView extends AbstractWindowTest {

    @Test
    public void testVGLongname() {
        String[][] expectedData =
            {   { "-127" },
                { "-127" },
                { "-127" },
                { "-127" },
                { "-127" },
                { "-127" },
                { "-127" },
                { "-127" },
                { "-127" },
                { "-127" } };
        SWTBotShell tableShell = null;
        String filename = "VGlongname.hdf";
        String groupname = "SD Vgroup - this vgroup has an sds as a member and it is actually meant to test long vgroup name";
        String datasetName = "SDS belongs to VG_LONGNAME, which has a very long name that is used to test the new feature of variable length vgroup name";
        File hdfFile = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "testVGLongname()", 2, filename);

            // Test metadata
            SWTBotTabItem tabItem = openMetadataTab(filetree, filename, groupname, "General Object Info");
            tabItem.activate();

            String val = bot.textWithLabel("Name: ").getText();
            assertTrue(constructWrongValueMessage("testVSLongname()", "wrong name", groupname, val), val.equals(groupname)); // Test group name

            // Open dataset
            tableShell = openTreeviewObject(filetree, filename, groupname + "/" + datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "testVGLongname()", false);
            retriever.setContainerHeaderOffset(2, 0);

            retriever.testAllTableLocations(expectedData);

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
    public void testVSLongname() {
        SWTBotShell tableShell = null;
        String filename = "vslongname.hdf";
        String datasetName = "Vdata 2 91123456789212345678931234567894123456789512345678961234";
        File hdfFile = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "testVSLongname()", 3, filename);

            // Test metadata
            SWTBotTabItem tabItem = openMetadataTab(filetree, filename, datasetName, "General Object Info");
            tabItem.activate();

            String val = bot.textWithLabel("Name: ").getText();
            assertTrue(constructWrongValueMessage("testVSLongname()", "wrong name", datasetName, val), val.equals(datasetName)); // Test dataset name

            val = bot.textInGroup("Dataset Dataspace and Datatype", 0).getText();
            assertTrue(constructWrongValueMessage("testVSLongname()", "wrong rank", "1", val),
                    val.equals("1"));           // Test rank

            val = bot.textInGroup("Dataset Dataspace and Datatype", 3).getText();
            assertTrue(constructWrongValueMessage("testVSLongname()", "wrong data type", "Vdata", val),
                    val.equals("Vdata"));   // Test data type
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
