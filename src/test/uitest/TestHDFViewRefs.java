package test.uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.junit.Test;

import test.uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

public class TestHDFViewRefs extends AbstractWindowTest {
    @Test
    public void openTAttributeRegionReference() {
        String[][] expectedAttrData = { { "Attribute1", "Dataset region reference", "4",
                "/Dataset2 REGION_TYPE BLOCK \\{ \\(2,2\\)\\-\\(7,7\\)  \\}, /Dataset2 REGION_TYPE POINT \\{ \\(6,9\\)  \\(2,2\\)  \\(8,4\\) , NULL, NULL" } };
        String[][] expectedData = {
                { "/Dataset2 REGION_TYPE BLOCK \\{ \\(2,2\\)\\-\\(7,7\\)  \\}" },
                { "/Dataset2 REGION_TYPE POINT \\{ \\(6,9\\)  \\(2,2\\)  \\(8,4\\)  \\(1,6\\)  \\(2,8\\)  \\(3,2\\)  \\(0,4\\)  \\(9,0\\)  \\(7,1\\)  \\(3,3\\)  \\}" },
                { "NULL" }, { "NULL" } };
        SWTBotShell tableShell = null;
        String filename = "tattrreg.h5";
        String datasetName = "/Dataset1";
        File hdfFile = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "openTAttributeRegionReference()", 3, filename);

            // Open dataset 'Dataset1' Attribute Table
            SWTBotTable attrTable = openAttributeTable(filetree, filename, datasetName);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(attrTable, "openTAttributeRegionReference()");

            retriever.testAllTableLocations(expectedAttrData);

            tableShell = openAttributeObject(attrTable, "Attribute1", 1);
            final SWTBotNatTable dataTable = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openTAttributeRegionReference()");

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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openTDataRegionReference() {
        String[][] expectedData = { { "/Dataset2 REGION_TYPE BLOCK \\{ \\(2,2\\)\\-\\(7,7\\)  \\}" },
                { "/Dataset2 REGION_TYPE POINT \\{ \\(6,9\\)  \\(2,2\\)  \\(8,4\\)  \\(1,6\\)  \\(2,8\\)  \\(3,2\\)  \\(0,4\\)  \\(9,0\\)  \\(7,1\\)  \\(3,3\\)  \\}" },
                { "NULL" }, { "NULL" } };
        SWTBotShell tableShell = null;
        String filename = "tdatareg.h5";
        String datasetName = "/Dataset1";
        File hdfFile = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "openTDataRegionReference()", 3, filename);

            // Test metadata
            SWTBotTabItem tabItem = openMetadataTab(filetree, filename, datasetName, "General Object Info");
            tabItem.activate();

            String val = bot.textWithLabel("Name: ").getText();
            assertTrue(constructWrongValueMessage("openTDataRegionReference()", "wrong name", datasetName, val), val.equals(datasetName)); // Test dataset name

            val = bot.textInGroup("Dataset Dataspace and Datatype", 0).getText();
            assertTrue(constructWrongValueMessage("openTDataRegionReference()", "wrong rank", "1", val),
                    val.equals("1"));           // Test rank

            val = bot.textInGroup("Dataset Dataspace and Datatype", 3).getText();
            assertTrue(constructWrongValueMessage("openTDataRegionReference()", "wrong data type", "Dataset region reference", val),
                    val.equals("Dataset region reference"));   // Test data type


            // Open dataset 'DS08BITS'
            tableShell = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openTDataRegionReference()");

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
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
