package uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;

import hdf.view.ViewProperties;

import org.junit.Ignore;
import org.junit.Test;

import uitest.AbstractWindowTest.DataRetrieverFactory.TableDataRetriever;

public class TestHDFViewRefs extends AbstractWindowTest {
    @Test
    public void openTAttributeRegionReference() {
        String[][] expectedAttrData = { { "Attribute1", "Dataset region reference", "4",
                "/Dataset2 REGION_TYPE BLOCK { (2,2)-(7,7) }, /Dataset2 REGION_TYPE POINT { (6,9) (2,2) (8,4) (1,6) (2,8) (3,2) (0,4) (9,0) (7,1) (3,3) }, NULL, NULL" } };
        String[][] expectedTrueData = {
                { "66,69,72,75,78,81,96,99,102,105,108,111,126,129,132,135,138,141,156,159,162,165,168,171,186,189,192,195,198,201,216,219,222,225,228,231" },
                { "207,66,252,48,84,96,12,14,213,99" },
                { "NULL" }, { "NULL" } };
        String[][] expectedData = {
                { "/Dataset2 REGION_TYPE BLOCK { (2,2)-(7,7) }" },
                { "/Dataset2 REGION_TYPE POINT { (6,9) (2,2) (8,4) (1,6) (2,8) (3,2) (0,4) (9,0) (7,1) (3,3) }" },
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

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(attrTable, "openTAttributeRegionReference()", true);

            retriever.testAllTableLocations(expectedAttrData);

            tableShell = openAttributeObject(attrTable, "Attribute1", 0);
            final SWTBotNatTable dataTable = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openTAttributeRegionReference()", true);

            boolean displayValues = ViewProperties.showRegRefValues();
            if (displayValues)
                retriever.testAllTableLocations(expectedTrueData);
            else
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
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
        String[][] expectedTrueData = { { "66,69,72,75,78,81,96,99,102,105,108,111,126,129,132,135,138,141,156,159,162,165,168,171,186,189,192,195,198,201,216,219,222,225,228,231" },
                { "207,66,252,48,84,96,12,14,213,99" },
                { "NULL" }, { "NULL" } };
        String[][] expectedTableData = { { "66","69","72","75","78","81" },
                { "96","99","102","105","108","111" },
                { "126","129","132","135","138","141" },
                { "156","159","162","165","168","171" },
                { "186","189","192","195","198","201" },
                { "216","219","222","225","228","231" } };
//        String[][] expectedTableData = { { "66,69,72,75,78,81,96,99,102,105,108,111,126,129,132,135,138,141,156,159,162,165,168,171,186,189,192,195,198,201,216,219,222,225,228,231" } };
        String[][] expectedData = { { "/Dataset2 REGION_TYPE BLOCK { (2,2)-(7,7) }" },
                { "/Dataset2 REGION_TYPE POINT { (6,9) (2,2) (8,4) (1,6) (2,8) (3,2) (0,4) (9,0) (7,1) (3,3) }" },
                { "NULL" }, { "NULL" } };
        SWTBotShell tableShell = null;
        SWTBotShell tableShellData = null;
        String filename = "tdatareg.h5";
        String datasetName = "Dataset1";
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

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openTDataRegionReference()", true);

            boolean displayValues = ViewProperties.showRegRefValues();
            if (displayValues)
                retriever.testAllTableLocations(expectedTrueData);
            else
                retriever.testAllTableLocations(expectedData);
            dataTable.doubleclick(1,1);
            tableShellData = openDataObject("Dataset2");

            final SWTBotNatTable table2 = getNatTable(tableShellData);
            TableDataRetriever retriever2 = DataRetrieverFactory.getTableDataRetriever(table2, "openTDataRegionReference()", true);

            //retriever2.testAllTableLocations(expectedTableData);
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
            if(tableShellData != null && tableShellData.isOpen()) {
                tableShellData.bot().menu().menu("Table").menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShellData));
            }
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.activate();
                bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
    public void openTAttributeReference() {
        String[][] expectedData = {
                { "trefer_attr.h5/Group1/Dataset1/Attr1 H5R_ATTR" },
                { "trefer_attr.h5/Group1/Dataset2/Attr1 H5R_ATTR" },
                { "trefer_attr.h5/Group1/Attr2 H5R_ATTR" },
                { "trefer_attr.h5/Group1/Datatype1/Attr3 H5R_ATTR" } };
        String[][] expectedAttrData = { { "0" }, { "3" }, { "6" }, { "9" } };
        SWTBotShell tableShell = null;
        SWTBotShell tableAttrShell = null;
        String filename = "trefer_attr.h5";
        String datasetName = "Dataset3";
        String attributeName = "Attr1";
        File hdfFile = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "openTAttributeReference()", 3, filename);

            // Test metadata
            SWTBotTabItem tabItem = openMetadataTab(filetree, filename, datasetName, "General Object Info");
            tabItem.activate();

            String val = bot.textWithLabel("Name: ").getText();
            assertTrue(constructWrongValueMessage("openTAttributeReference()", "wrong name", datasetName, val), val.equals(datasetName)); // Test dataset name

            val = bot.textInGroup("Dataset Dataspace and Datatype", 0).getText();
            assertTrue(constructWrongValueMessage("openTAttributeReference()", "wrong rank", "1", val),
                    val.equals("1"));           // Test rank

            val = bot.textInGroup("Dataset Dataspace and Datatype", 3).getText();
            assertTrue(constructWrongValueMessage("openTAttributeReference()", "wrong data type", "Reference", val),
                    val.equals("Reference"));   // Test data type

            // Open dataset 'Dataset3' Table
            tableShell = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openTAttributeReference()", true);

            retriever.testAllTableLocations(expectedData);

            // Open attribute 'Attr1' Table
            dataTable.doubleclick(1, 1);
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(attributeName + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu().menu("Table").menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                tableAttrShell = bot.shells()[1];
                tableAttrShell.activate();
                bot.waitUntil(Conditions.shellIsActive(tableAttrShell.getText()));

                SWTBotNatTable attributeTable = getNatTable(tableAttrShell);

                retriever = DataRetrieverFactory.getTableDataRetriever(attributeTable, "openTAttributeReference()", true);

                retriever.testAllTableLocations(expectedAttrData);
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
                if(tableAttrShell != null && tableAttrShell.isOpen()) {
                    tableAttrShell.bot().menu().menu("Table").menu("Close").click();
                    bot.waitUntil(Conditions.shellCloses(tableAttrShell));
                }
            }
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
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
    public void openTObjectReference() {
        String[][] expectedData = { { "trefer_obj.h5/Group1/Dataset1 H5O_TYPE_OBJ_REF" },
                                    { "trefer_obj.h5/Group1/Dataset2 H5O_TYPE_OBJ_REF" },
                                    { "trefer_obj.h5/Group1 H5O_TYPE_OBJ_REF" },
                                    { "trefer_obj.h5/Group1/Datatype1 H5O_TYPE_OBJ_REF" } };
        String[][] expectedObjData = { { "0" }, { "3" }, { "6" }, { "9" } };
        SWTBotShell tableShell = null;
        SWTBotShell tableObjShell = null;
        String filename = "trefer_obj.h5";
        String datasetName = "Dataset3";
        String objectName = "Dataset1";
        File hdfFile = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();

            checkFileTree(filetree, "openTObjectReference()", 3, filename);

            // Test metadata
            SWTBotTabItem tabItem = openMetadataTab(filetree, filename, datasetName, "General Object Info");
            tabItem.activate();

            String val = bot.textWithLabel("Name: ").getText();
            assertTrue(constructWrongValueMessage("openTObjectReference()", "wrong name", datasetName, val), val.equals(datasetName)); // Test dataset name

            val = bot.textInGroup("Dataset Dataspace and Datatype", 0).getText();
            assertTrue(constructWrongValueMessage("openTObjectReference()", "wrong rank", "1", val),
                    val.equals("1"));           // Test rank

            val = bot.textInGroup("Dataset Dataspace and Datatype", 3).getText();
            assertTrue(constructWrongValueMessage("openTObjectReference()", "wrong data type", "Reference", val),
                    val.equals("Reference"));   // Test data type

            // Open dataset 'Dataset3' Table
            tableShell = openTreeviewObject(filetree, filename, datasetName);
            final SWTBotNatTable dataTable = getNatTable(tableShell);

            TableDataRetriever retriever = DataRetrieverFactory.getTableDataRetriever(dataTable, "openTObjectReference()", true);

            retriever.testAllTableLocations(expectedData);

            // Open object 'Dataset1' Table
            dataTable.doubleclick(1, 1);
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(objectName + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));
            if(tableShell != null && tableShell.isOpen()) {
                tableShell.bot().menu().menu("Table").menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                tableObjShell = bot.shells()[1];
                tableObjShell.activate();
                bot.waitUntil(Conditions.shellIsActive(tableObjShell.getText()));

                SWTBotNatTable objectTable = getNatTable(tableObjShell);

                retriever = DataRetrieverFactory.getTableDataRetriever(objectTable, "openTObjectReference()", true);

                retriever.testAllTableLocations(expectedObjData);
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
                if(tableObjShell != null && tableObjShell.isOpen()) {
                    tableObjShell.bot().menu().menu("Table").menu("Close").click();
                    bot.waitUntil(Conditions.shellCloses(tableObjShell));
                }
            }
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
                tableShell.bot().menu().menu("Table").menu("Close").click();
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
