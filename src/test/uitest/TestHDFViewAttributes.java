package test.uitest;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;
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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

/*
 * NOTE: Currently no support is added for testing Compound type Attributes.
 */

public class TestHDFViewAttributes extends AbstractWindowTest {
    private String groupname = "test_group";
    private String datasetname = "test_dataset";

    /*
     * Constants to keep track of the order of the columns in the attribute table,
     * in case these change in the future.
     */
    private static final int ATTRIBUTE_TABLE_NAME_COLUMN_INDEX = 0;
    private static final int ATTRIBUTE_TABLE_TYPE_COLUMN_INDEX = 1;
    private static final int ATTRIBUTE_TABLE_ARRAY_SIZE_COLUMN_INDEX = 2;
    private static final int ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX = 3;

    private void createNewGroup() {
        SWTBotShell groupShell = null;

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].click();
            items[0].contextMenu("New").menu("Group").click();

            groupShell = bot.shell("New Group...");
            groupShell.activate();
            bot.waitUntil(Conditions.shellIsActive(groupShell.getText()));

            groupShell.bot().text(0).setText(groupname);

            String val = groupShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("createNewGroup()", "wrong group name", groupname, val),
                    val.equals(groupname));

            groupShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(groupShell));
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
            if (groupShell != null && groupShell.isOpen()) {
                groupShell.close();
                bot.waitUntil(Conditions.shellCloses(groupShell));
            }
        }
    }

    private void createNewDataset() {
        String currentSize = "4 x 4";
        SWTBotShell tableShell = null;
        SWTBotShell datasetShell = null;

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu("New").menu("Dataset").click();

            datasetShell = bot.shell("New Dataset...");
            datasetShell.activate();
            bot.waitUntil(Conditions.shellIsActive(datasetShell.getText()));

            datasetShell.bot().text(0).setText(datasetname);

            String val = datasetShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("createNewDataset()", "wrong dataset name", datasetname, val),
                    val.equals(datasetname));

            datasetShell.bot().text(2).setText(currentSize);

            val = datasetShell.bot().text(2).getText();
            assertTrue(constructWrongValueMessage("createNewDataset()", "wrong current size", currentSize, val),
                    val.equals(currentSize));

            datasetShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(datasetShell));

            filetree.expandNode(items[0].getText(), true);
            items = filetree.getAllItems();

            items[0].getNode(0).getNode(0).click();
            items[0].getNode(0).getNode(0).contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(datasetname + ".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(widgetOfType(NatTable.class)));

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    for (int row = 1; row <= 4; row++) {
                        for (int col = 1; col <= 4; col++) {
                            String val = String.valueOf(((row - 1) * 4) + (col));
                            table.doubleclick(row, col);
                            table.widget.getActiveCellEditor().setEditorValue(val);
                            table.widget.getActiveCellEditor().commit(SelectionLayer.MoveDirectionEnum.RIGHT, true,
                                    true);
                            assertTrue(
                                    constructWrongValueMessage("createNewDataset()", "wrong value", val,
                                            table.getCellDataValueByPosition(row, col)),
                                    table.getCellDataValueByPosition(row, col).equals(val));
                        }
                    }
                }
            });

            tableShell.bot().menu("Table").menu("Save Changes to File").click();

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
            if (datasetShell != null && datasetShell.isOpen()) {
                datasetShell.close();
                bot.waitUntil(Conditions.shellCloses(datasetShell));
            }

            if (tableShell != null && tableShell.isOpen()) {
                tableShell.close();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }
        }
    }

    @Test
    public void testAddHDF4Attribute() {
        String filename = "testaddattribute";
        String file_ext = ".hdf";
        String attrName = "test_attribute";
        String attrValue = "13";
        SWTBotShell newAttributeShell = null;

        File hdf_file = createHDF4File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testAddHDF4Attribute()", "filetree wrong row count", "1",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 1);
            assertTrue("testAddHDF4Attribute() filetree is missing file '" + filename + file_ext + "'",
                    items[0].getText().compareTo(filename + file_ext) == 0);

            createNewGroup();
            createNewDataset();

            assertTrue(constructWrongValueMessage("testAddHDF4Attribute()", "filetree wrong row count", "3",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 3);
            assertTrue("testAddHDF4Attribute() filetree is missing group '" + groupname + "'",
                    items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testAddHDF4Attribute() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            /* Add an attribute to the newly-created group */
            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the group */
            SWTBotTable attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testAddHDF4Attribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            SWTBotButton addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testAddHDF4Attribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            SWTBotTableItem newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testAddHDF4Attribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testAddHDF4Attribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Now repeat the process for the dataset that was created */
            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there is only the "_FillValue" attribute on the dataset */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testAddHDF4Attribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testAddHDF4Attribute()", "attribute table wrong row count", "2",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 2);

            /* Grab the second attribute, since "_FillValue" should be first */
            newItem = attrTable.getTableItem(1);

            assertTrue(
                    constructWrongValueMessage("testAddHDF4Attribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testAddHDF4Attribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));
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
            if (newAttributeShell != null && newAttributeShell.isOpen()) {
                newAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(newAttributeShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testAddHDF5Attribute() {
        String filename = "testaddattribute";
        String file_ext = ".h5";
        String attrName = "test_attribute";
        String attrValue = "13";
        SWTBotShell newAttributeShell = null;

        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testAddHDF5Attribute()", "filetree wrong row count", "1",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 1);
            assertTrue("testAddHDF5Attribute() filetree is missing file '" + filename + file_ext + "'",
                    items[0].getText().compareTo(filename + file_ext) == 0);

            createNewGroup();
            createNewDataset();

            assertTrue(constructWrongValueMessage("testAddHDF5Attribute()", "filetree wrong row count", "3",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 3);
            assertTrue("testAddHDF5Attribute() filetree is missing group '" + groupname + "'",
                    items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testAddHDF5Attribute() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            /* Add an attribute to the newly-created group */
            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the group */
            SWTBotTable attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testAddHDF5Attribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            SWTBotButton addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /* Verify that the attribute has been added to the table with the correct name and value */
            assertTrue(constructWrongValueMessage("testAddHDF5Attribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            SWTBotTableItem newItem = attrTable.getTableItem(0);

            assertTrue(constructWrongValueMessage("testAddHDF5Attribute()", "attribute wrong name", attrName,
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(constructWrongValueMessage("testAddHDF5Attribute()", "attribute wrong value", attrValue,
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Now repeat the process for the dataset that was created */
            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the dataset */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testAddHDF5Attribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testAddHDF5Attribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage(
                            "testAddHDF5Attribute()",
                            "attribute wrong name",
                            attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)
                            ),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName)
                    );
            assertTrue(
                    constructWrongValueMessage(
                            "testAddHDF5Attribute()",
                            "attribute wrong value",
                            attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)
                            ),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue)
                    );
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
            if (newAttributeShell != null && newAttributeShell.isOpen()) {
                newAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(newAttributeShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testHDF5DeleteAttribute() {
        String filename = "testdeleteattribute";
        String file_ext = ".h5";
        String attrName = "test_attribute";
        String attrValue = "13";
        SWTBotShell newAttributeShell = null;
        SWTBotShell deleteAttributeShell = null;

        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "filetree wrong row count", "1",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 1);
            assertTrue("testHDF5DeleteAttribute() filetree is missing file '" + filename + file_ext + "'",
                    items[0].getText().compareTo(filename + file_ext) == 0);

            createNewGroup();
            createNewDataset();

            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "filetree wrong row count", "3",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 3);
            assertTrue("testHDF5DeleteAttribute() filetree is missing group '" + groupname + "'",
                    items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testHDF5DeleteAttribute() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            /* Test deletion of attribute by button */

            /* Add an attribute to the newly-created group */
            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the group */
            SWTBotTable attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            SWTBotButton addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            SWTBotTableItem newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Now attempt to delete the attribute */

            newItem.click();
            bot.button("Delete Attribute").click();

            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*" + VERSION + " - Delete");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            deleteAttributeShell = bot.shell("HDFView " + VERSION + " - Delete");
            deleteAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(deleteAttributeShell.getText()));

            deleteAttributeShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(deleteAttributeShell));

            /*
             * Verify that the attribute has been removed from the attribute table.
             */
            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            /* Now repeat the process for the dataset created previously */

            /* Add an attribute to the newly-created dataset */
            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the dataset */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Now attempt to delete the attribute */

            newItem.click();
            bot.button("Delete Attribute").click();

            shellMatcher = WithRegex.withRegex(".*" + VERSION + " - Delete");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            deleteAttributeShell = bot.shell("HDFView " + VERSION + " - Delete");
            deleteAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(deleteAttributeShell.getText()));

            deleteAttributeShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(deleteAttributeShell));

            /*
             * Verify that the attribute has been removed from the attribute table.
             */
            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);


            /* Now test deletion of attributes by using the popup menu */

            /* Reload the file for good measure */
            items[0].click();
            items[0].contextMenu().menu("&Reload File As").menu("Read/Write").click();

            items = bot.tree().getAllItems();
            items[0].contextMenu("Expand All").click();

            /* Add an attribute to the newly-created group */
            items[0].getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the group */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Now attempt to delete the attribute */

            newItem.click();
            bot.table().contextMenu("Delete Attribute").click();

            shellMatcher = WithRegex.withRegex(".*" + VERSION + " - Delete");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            deleteAttributeShell = bot.shell("HDFView " + VERSION + " - Delete");
            deleteAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(deleteAttributeShell.getText()));

            deleteAttributeShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(deleteAttributeShell));

            /*
             * Verify that the attribute has been removed from the attribute table.
             */
            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            /* Now repeat the process for the dataset created previously */

            /* Add an attribute to the newly-created dataset */
            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the dataset */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Now attempt to delete the attribute */

            newItem.click();
            bot.table().contextMenu("Delete Attribute").click();

            shellMatcher = WithRegex.withRegex(".*" + VERSION + " - Delete");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            deleteAttributeShell = bot.shell("HDFView " + VERSION + " - Delete");
            deleteAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(deleteAttributeShell.getText()));

            deleteAttributeShell.bot().button("OK").click();
            bot.waitUntil(Conditions.shellCloses(deleteAttributeShell));

            /*
             * Verify that the attribute has been removed from the attribute table.
             */
            assertTrue(constructWrongValueMessage("testHDF5DeleteAttribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);
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
            if (newAttributeShell != null && newAttributeShell.isOpen()) {
                newAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(newAttributeShell));
            }

            if (deleteAttributeShell != null && deleteAttributeShell.isOpen()) {
                deleteAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(deleteAttributeShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testHDF5RenameAttribute() {
        String filename = "testrenameattribute";
        String file_ext = ".h5";
        String attrName = "test_attribute";
        String newAttrName = "renamed_attribute";
        String attrValue = "13";
        SWTBotShell newAttributeShell = null;
        SWTBotShell renameAttributeShell = null;

        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testHDF5RenameAttribute()", "filetree wrong row count", "1",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 1);
            assertTrue("testHDF5RenameAttribute() filetree is missing file '" + filename + file_ext + "'",
                    items[0].getText().compareTo(filename + file_ext) == 0);

            createNewGroup();
            createNewDataset();

            assertTrue(constructWrongValueMessage("testHDF5RenameAttribute()", "filetree wrong row count", "3",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 3);
            assertTrue("testHDF5RenameAttribute() filetree is missing group '" + groupname + "'",
                    items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testHDF5RenameAttribute() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            /* Add an attribute to the newly-created group */
            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the group */
            SWTBotTable attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testHDF5RenameAttribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            SWTBotButton addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testHDF5RenameAttribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            SWTBotTableItem newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Now attempt to rename the attribute */

            newItem.click();
            bot.table().contextMenu("Rename Attribute").click();

            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*" + VERSION + " - Rename Attribute");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            renameAttributeShell = bot.shell("HDFView " + VERSION + " - Rename Attribute");
            renameAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(renameAttributeShell.getText()));

            renameAttributeShell.bot().text().setText(newAttrName);

            renameAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(renameAttributeShell));

            /* Verify that the attribute has been renamed */
            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong name", newAttrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(newAttrName));
            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Reload the file for good measure to make sure the changes are saved */
            items[0].click();
            items[0].contextMenu("Reload File As").menu("Read/Write").click();

            items = bot.tree().getAllItems();
            items[0].contextMenu("Expand All").click();

            items[0].getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that the attribute is still on the group */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testHDF5RenameAttribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            newItem = attrTable.getTableItem(0);

            /* Verify that the attribute has been renamed */
            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong name", newAttrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(newAttrName));
            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Now repeat the process for the previously-created dataset */

            /* Add an attribute to the newly-created dataset */
            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the dataset */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testHDF5RenameAttribute()", "attribute table wrong row count", "0",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testHDF5RenameAttribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Now attempt to rename the attribute */

            newItem.click();
            bot.table().contextMenu("Rename Attribute").click();

            shellMatcher = WithRegex.withRegex(".*" + VERSION + " - Rename Attribute");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            renameAttributeShell = bot.shell("HDFView " + VERSION + " - Rename Attribute");
            renameAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(renameAttributeShell.getText()));

            renameAttributeShell.bot().text().setText(newAttrName);

            renameAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(renameAttributeShell));

            /* Verify that the attribute has been renamed */
            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong name", newAttrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(newAttrName));
            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Reload the file for good measure to make sure the changes are saved */
            items[0].click();
            items[0].contextMenu("Reload File As").menu("Read/Write").click();

            items = bot.tree().getAllItems();
            items[0].contextMenu("Expand All").click();

            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that the attribute is still on the dataset */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testHDF5RenameAttribute()", "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            newItem = attrTable.getTableItem(0);

            /* Verify that the attribute has been renamed */
            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong name", newAttrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(newAttrName));
            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));
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
            if (newAttributeShell != null && newAttributeShell.isOpen()) {
                newAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(newAttributeShell));
            }

            if (renameAttributeShell != null && renameAttributeShell.isOpen()) {
                renameAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(renameAttributeShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testOpenHDF4ScalarAttribute() {
        String filename = "testopenattribute";
        String file_ext = ".hdf";
        String attrName = "test_attribute";
        String attrValue = "13";
        SWTBotShell newAttributeShell = null;
        SWTBotShell openAttributeShell = null;

        File hdf_file = createHDF4File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "filetree wrong row count", "1",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 1);
            assertTrue("testOpenHDF4ScalarAttribute() filetree is missing file '" + filename + file_ext + "'",
                    items[0].getText().compareTo(filename + file_ext) == 0);

            createNewGroup();
            createNewDataset();

            assertTrue(constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "filetree wrong row count", "3",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 3);
            assertTrue("testOpenHDF4ScalarAttribute() filetree is missing group '" + groupname + "'",
                    items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testOpenHDF4ScalarAttribute() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            /* Add an attribute to the newly-created group */
            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the group */
            SWTBotTable attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute table wrong row count",
                    "0", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            SWTBotButton addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute table wrong row count",
                    "1", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            SWTBotTableItem newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Test open of attribute by double-click */
            newItem.doubleClick();

            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex("");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            openAttributeShell = bot.shells()[1];
            openAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(openAttributeShell.getText()));

            openAttributeShell.close();

            /* Now repeat the process for the previously-created dataset */

            /* Add an attribute to the newly-created dataset */
            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the dataset */
            attrTable = bot.table();

            /* HDF4 datasets have the "_FillValue" attribute attached */
            assertTrue(constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute table wrong row count",
                    "1", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute table wrong row count",
                    "2", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 2);

            newItem = attrTable.getTableItem(1);

            assertTrue(
                    constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Test open of attribute by double-click */
            newItem.doubleClick();

            shellMatcher = WithRegex.withRegex("");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            openAttributeShell = bot.shells()[1];
            openAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(openAttributeShell.getText()));

            openAttributeShell.close();

            /* Test open of attribute by popup menu */

            /* Reload file for good measure */
            items[0].click();
            items[0].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            items[0].contextMenu("Expand All").click();

            items[0].getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that attribute is still on the group */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute table wrong row count",
                    "1", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            /*
             * Verify that the attribute has the correct name and value
             */
            newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            newItem.click();
            attrTable.contextMenu("View/Edit Attribute Value").click();

            shellMatcher = WithRegex.withRegex("");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            openAttributeShell = bot.shells()[1];
            openAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(openAttributeShell.getText()));

            openAttributeShell.close();

            /* Now repeat the process for the previously-created dataset */

            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that attribute is still on the dataset */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute table wrong row count",
                    "2", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 2);

            /*
             * Verify that the attribute has the correct name and value
             */
            newItem = attrTable.getTableItem(1);

            assertTrue(
                    constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testOpenHDF4ScalarAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            newItem.click();
            attrTable.contextMenu("View/Edit Attribute Value").click();

            shellMatcher = WithRegex.withRegex("");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            openAttributeShell = bot.shells()[1];
            openAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(openAttributeShell.getText()));

            openAttributeShell.close();
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
            if (newAttributeShell != null && newAttributeShell.isOpen()) {
                newAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(newAttributeShell));
            }

            if (openAttributeShell != null && openAttributeShell.isOpen()) {
                openAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(openAttributeShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testOpenHDF5ScalarAttribute() {
        String filename = "testopenattribute";
        String file_ext = ".h5";
        String attrName = "test_attribute";
        String attrValue = "13";
        SWTBotShell newAttributeShell = null;
        SWTBotShell openAttributeShell = null;

        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "filetree wrong row count", "1",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 1);
            assertTrue("testOpenHDF5ScalarAttribute() filetree is missing file '" + filename + file_ext + "'",
                    items[0].getText().compareTo(filename + file_ext) == 0);

            createNewGroup();
            createNewDataset();

            assertTrue(constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "filetree wrong row count", "3",
                    String.valueOf(filetree.visibleRowCount())), filetree.visibleRowCount() == 3);
            assertTrue("testOpenHDF5ScalarAttribute() filetree is missing group '" + groupname + "'",
                    items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testOpenHDF5ScalarAttribute() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            /* Add an attribute to the newly-created group */
            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the group */
            SWTBotTable attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute table wrong row count",
                    "0", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            SWTBotButton addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute table wrong row count",
                    "1", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            SWTBotTableItem newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Test open of attribute by double-click */
            newItem.doubleClick();

            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex("");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            openAttributeShell = bot.shells()[1];
            openAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(openAttributeShell.getText()));

            openAttributeShell.close();

            /* Now repeat the process for the previously-created dataset */

            /* Add an attribute to the newly-created dataset */
            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that there are currently no attributes on the dataset */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute table wrong row count",
                    "0", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 0);

            addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /*
             * Verify that the attribute has been added to the table with the correct name
             * and value
             */
            assertTrue(constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute table wrong row count",
                    "1", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            /* Test open of attribute by double-click */
            newItem.doubleClick();

            shellMatcher = WithRegex.withRegex("");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            openAttributeShell = bot.shells()[1];
            openAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(openAttributeShell.getText()));

            openAttributeShell.close();

            /* Test open of attribute by popup menu */

            /* Reload file for good measure */
            items[0].click();
            items[0].contextMenu("Reload File").click();

            items = filetree.getAllItems();
            items[0].contextMenu("Expand All").click();

            items[0].getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that attribute is still on the group */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute table wrong row count",
                    "1", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            /*
             * Verify that the attribute has the correct name and value
             */
            newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            newItem.click();
            attrTable.contextMenu("View/Edit Attribute Value").click();

            shellMatcher = WithRegex.withRegex("");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            openAttributeShell = bot.shells()[1];
            openAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(openAttributeShell.getText()));

            openAttributeShell.close();

            /* Now repeat the process for the previously-created dataset */

            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            /* Verify that attribute is still on the dataset */
            attrTable = bot.table();

            assertTrue(constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute table wrong row count",
                    "1", String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);

            /*
             * Verify that the attribute has the correct name and value
             */
            newItem = attrTable.getTableItem(0);

            assertTrue(
                    constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute wrong name", attrName,
                            newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            assertTrue(
                    constructWrongValueMessage("testOpenHDF5ScalarAttribute()", "attribute wrong value", attrValue,
                            newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));

            newItem.click();
            attrTable.contextMenu("View/Edit Attribute Value").click();

            shellMatcher = WithRegex.withRegex("");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            openAttributeShell = bot.shells()[1];
            openAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(openAttributeShell.getText()));

            openAttributeShell.close();
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
            if (newAttributeShell != null && newAttributeShell.isOpen()) {
                newAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(newAttributeShell));
            }

            if (openAttributeShell != null && openAttributeShell.isOpen()) {
                openAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(openAttributeShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testOpenHDF5CompoundAttribute() {
        try {
            /* Test open of attribute by double-click */

            /* Test open of attribute by popup menu */
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

    @Test
    public void testEditHDF4ScalarAttribute() {
        try {

        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

    @Test
    public void testEditHDF5ScalarAttribute() {
        try {

        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

    @Test
    public void testEditHDF5CompoundAttribute() {
        try {

        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

    @Test
    public void testDiscardHDF4ScalarAttributeEditResults() {
        try {

        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

    @Test
    public void testDiscardHDF5ScalarAttributeEditResults() {
        try {

        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

    @Test
    public void testDiscardHDF5CompoundAttributeEditResults() {
        try {

        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

    @Test
    public void testHDF4RenameAttributeFunctionDisabled() {
        String filename = "testrenameattributedisabled";
        String file_ext = ".hdf";

        File hdf_file = createHDF4File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testHDF4RenameAttributeFunctionDisabled()",
                    "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue(
                    "testHDF4RenameAttributeFunctionDisabled() filetree is missing file '" + filename + file_ext + "'",
                    items[0].getText().compareTo(filename + file_ext) == 0);

            createNewGroup();
            createNewDataset();

            assertTrue(constructWrongValueMessage("testHDF4RenameAttributeFunctionDisabled()",
                    "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 3);
            assertTrue("testHDF4RenameAttributeFunctionDisabled() filetree is missing group '" + groupname + "'",
                    items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testHDF4RenameAttributeFunctionDisabled() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            assertTrue(
                    constructWrongValueMessage("testHDF4RenameAttributeFunctionDisabled()",
                            "rename attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu("Rename Attribute").isEnabled());

            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            assertTrue(
                    constructWrongValueMessage("testHDF4RenameAttributeFunctionDisabled()",
                            "rename attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu("Rename Attribute").isEnabled());
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
    public void testHDF4DeleteAttributeFunctionDisabled() {
        String filename = "testdeleteattributedisabled";
        String file_ext = ".hdf";

        File hdf_file = createHDF4File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("testHDF4DeleteAttributeFunctionDisabled()",
                    "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue(
                    "testHDF4DeleteAttributeFunctionDisabled() filetree is missing file '" + filename + file_ext + "'",
                    items[0].getText().compareTo(filename + file_ext) == 0);

            createNewGroup();
            createNewDataset();

            assertTrue(constructWrongValueMessage("testHDF4DeleteAttributeFunctionDisabled()",
                    "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 3);
            assertTrue("testHDF4DeleteAttributeFunctionDisabled() filetree is missing group '" + groupname + "'",
                    items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testHDF4DeleteAttributeFunctionDisabled() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            items[0].getNode(0).click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            assertTrue(
                    constructWrongValueMessage("testHDF4DeleteAttributeFunctionDisabled()",
                            "delete attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu("Delete Attribute").isEnabled());
            assertTrue(
                    constructWrongValueMessage("testHDF4DeleteAttributeFunctionDisabled()",
                            "delete attribute button not disabled", "disabled", "enabled"),
                    !bot.button("Delete Attribute").isEnabled());

            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            assertTrue(
                    constructWrongValueMessage("testHDF4DeleteAttributeFunctionDisabled()",
                            "delete attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu("Delete Attribute").isEnabled());
            assertTrue(
                    constructWrongValueMessage("testHDF4DeleteAttributeFunctionDisabled()",
                            "delete attribute button not disabled", "disabled", "enabled"),
                    !bot.button("Delete Attribute").isEnabled());
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
    public void testHDF4AttributeEditDisabledForReadOnly() {
        try {

        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

    @Test
    public void testHDF5RenameAttributeDisabledForReadOnly() {
        String filename = "testrenameattributedisabledreadonly";
        String file_ext = ".h5";
        SWTBotShell openFileShell = null;

        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttributeDisabledForReadOnly()",
                            "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 1);
            assertTrue("testHDF5RenameAttributeDisabledForReadOnly() filetree is missing file '" + filename + file_ext
                    + "'", items[0].getText().compareTo(filename + file_ext) == 0);

            createNewGroup();
            createNewDataset();

            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttributeDisabledForReadOnly()",
                            "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount() == 3);
            assertTrue("testHDF5RenameAttributeDisabledForReadOnly() filetree is missing group '" + groupname + "'",
                    items[0].getNode(0).getText().compareTo(groupname) == 0);
            assertTrue("testHDF5RenameAttributeDisabledForReadOnly() filetree is missing dataset '" + datasetname + "'",
                    items[0].getNode(0).getNode(0).getText().compareTo(datasetname) == 0);

            closeFile(hdf_file, false);

            bot.menu("File").menu("Open As").menu("Read-Only").click();

            SWTBotShell shell = bot.shell("Enter a file name");
            shell.activate();

            SWTBotText text = shell.bot().text();
            text.setText(hdf_file.getName());

            String val = text.getText();
            assertTrue("openFile() wrong file name: expected '" + hdf_file.getName() + "' but was '" + val + "'",
                    val.equals(hdf_file.getName()));

            shell.bot().button("   &OK   ").click();
            bot.waitUntil(shellCloses(shell));

            filetree = bot.tree();
            items = filetree.getAllItems();

            items[0].click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttributeDisabledForReadOnly()",
                            "rename attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu("Rename Attribute").isEnabled());

            items[0].getNode(0).getNode(0).click();

            tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttributeDisabledForReadOnly()",
                            "rename attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu("Rename Attribute").isEnabled());
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
            if (openFileShell != null && openFileShell.isOpen()) {
                openFileShell.close();
                bot.waitUntil(Conditions.shellCloses(openFileShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testHDF5AttributeEditDisabledForReadOnly() {
        try {

        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        catch (AssertionError ae) {
            ae.printStackTrace();
            fail(ae.getMessage());
        }
    }

}
