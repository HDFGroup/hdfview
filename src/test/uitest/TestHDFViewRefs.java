package test.uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

public class TestHDFViewRefs extends AbstractWindowTest {
    @Test
    public void openTAttributeRegionReference() {
        String filename = "tattrreg.h5";
        String dataset_name = "Dataset1";
        String dataset_name2 = "Dataset2";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTAttributeRegionReference()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==3);
            assertTrue("openTAttributeRegionReference() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openTAttributeRegionReference() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTAttributeRegionReference() filetree is missing dataset '" + dataset_name2 + "'", items[0].getNode(1).getText().compareTo(dataset_name2)==0);

            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            SWTBotTable table = new SWTBotTable(bot.widget(widgetOfType(Table.class)));

            table.click(0, 0);
            assertTrue("openTAttributeRegionReference() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(0,0)+"] did not match regex 'Attribute1'",
                    table.cell(0,0).matches("Attribute1"));

            assertTrue("openTAttributeRegionReference() data{"+table.rowCount()+","+table.columnCount()+"} ["+
                    table.cell(0,3)+"] did not match regex /Dataset2 REGION_TYPE BLOCK { (2,2)-(7,7)  }, /Dataset2 REGION_TYPE POINT { (6,9)  (2,2)  (8,4) , NULL, NULL'",
                    table.cell(0,3).matches("/Dataset2 REGION_TYPE BLOCK \\{ \\(2,2\\)\\-\\(7,7\\)  \\}, /Dataset2 REGION_TYPE POINT \\{ \\(6,9\\)  \\(2,2\\)  \\(8,4\\) , NULL, NULL"));

            table.doubleClick(0, 0);
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex("Attribute1.*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table2 = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            String val = table2.getCellDataValueByPosition(1, 1);
            assertTrue("openTAttributeRegionReference() data ["+val+
                    "] did not match regex '/Dataset2 REGION_TYPE BLOCK { (2,2)-(7,7)  }'",
                    val.matches("/Dataset2 REGION_TYPE BLOCK \\{ \\(2,2\\)\\-\\(7,7\\)  \\}"));

            val = table2.getCellDataValueByPosition(2, 1);
            assertTrue("openTAttributeRegionReference() data ["+val+
                    "] did not match regex '/Dataset2 REGION_TYPE POINT { (6,9)  (2,2)  (8,4)  (1,6)  (2,8)  (3,2)  (0,4)  (9,0)  (7,1)  (3,3)  }'",
                    val.matches("/Dataset2 REGION_TYPE POINT \\{ \\(6,9\\)  \\(2,2\\)  \\(8,4\\)  \\(1,6\\)  \\(2,8\\)  \\(3,2\\)  \\(0,4\\)  \\(9,0\\)  \\(7,1\\)  \\(3,3\\)  \\}"));

            val = table2.getCellDataValueByPosition(3, 1);
            assertTrue("openTAttributeRegionReference() data ["+val+
                    "] did not match regex 'NULL'",
                    val.matches("NULL"));

            val = table2.getCellDataValueByPosition(4, 1);
            assertTrue("openTAttributeRegionReference() data ["+val+
                    "] did not match regex 'NULL'",
                    val.matches("NULL"));

            tableShell.bot().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
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
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void openTDataRegionReference() {
        String filename = "tdatareg.h5";
        String dataset_name = "Dataset1";
        String dataset_name2 = "Dataset2";
        SWTBotShell tableShell = null;
        File hdf_file = openFile(filename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("openTDataRegionReference()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount()==3);
            assertTrue("openTDataRegionReference() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("openTDataRegionReference() filetree is missing dataset '" + dataset_name + "'", items[0].getNode(0).getText().compareTo(dataset_name)==0);
            assertTrue("openTDataRegionReference() filetree is missing dataset '" + dataset_name2 + "'", items[0].getNode(1).getText().compareTo(dataset_name2)==0);

            items[0].getNode(0).click();

            // Test metadata

            SWTBotTabItem tabItem = bot.tabItem("General Object Info");
            tabItem.activate();

            String val = bot.textWithLabel("Name: ").getText();
            assertTrue(constructWrongValueMessage("openTDataRegionReference()", "wrong name", dataset_name, val),
                    val.equals(dataset_name));       // Test dataset name

            val = bot.textInGroup("Dataset Dataspace and Datatype", 0).getText();
            assertTrue(constructWrongValueMessage("openTDataRegionReference()", "wrong rank", "1", val),
                    val.equals("1"));           // Test rank

            val = bot.textInGroup("Dataset Dataspace and Datatype", 3).getText();
            assertTrue(constructWrongValueMessage("openTDataRegionReference()", "wrong data type", "Dataset region reference", val),
                    val.equals("Dataset region reference"));   // Test data type

            items[0].getNode(0).contextMenu().menu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(dataset_name + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            val = table.getCellDataValueByPosition(1, 1);
            assertTrue("openTDataRegionReference() data ["+val+
                    "] did not match regex '/Dataset2 REGION_TYPE BLOCK { (2,2)-(7,7)  }'",
                    val.matches("/Dataset2 REGION_TYPE BLOCK \\{ \\(2,2\\)\\-\\(7,7\\)  \\}"));

            val = table.getCellDataValueByPosition(2, 1);
            assertTrue("openTDataRegionReference() data ["+val+
                    "] did not match regex '/Dataset2 REGION_TYPE POINT { (6,9)  (2,2)  (8,4)  (1,6)  (2,8)  (3,2)  (0,4)  (9,0)  (7,1)  (3,3)  }'",
                    val.matches("/Dataset2 REGION_TYPE POINT \\{ \\(6,9\\)  \\(2,2\\)  \\(8,4\\)  \\(1,6\\)  \\(2,8\\)  \\(3,2\\)  \\(0,4\\)  \\(9,0\\)  \\(7,1\\)  \\(3,3\\)  \\}"));

            val = table.getCellDataValueByPosition(3, 1);
            assertTrue("openTDataRegionReference() data ["+val+
                    "] did not match regex 'NULL'",
                    val.matches("NULL"));

            val = table.getCellDataValueByPosition(4, 1);
            assertTrue("openTDataRegionReference() data ["+val+
                    "] did not match regex 'NULL'",
                    val.matches("NULL"));

            tableShell.bot().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
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
                closeFile(hdf_file, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
