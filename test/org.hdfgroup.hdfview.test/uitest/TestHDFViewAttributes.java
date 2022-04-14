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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Ignore;
import org.junit.Test;

/*
 * NOTE: Currently no support is added for testing Compound type Attributes.
 */

public class TestHDFViewAttributes extends AbstractWindowTest {
    private String groupname = "test_group";
    private String datasetname = "test_dataset";
    private String attrName = "test_attribute";
    private String attrDimsize = "2 x 2";
    private String attrValue = "13";

    /*
     * Constants to keep track of the order of the columns in the attribute table,
     * in case these change in the future.
     */
    private static final int ATTRIBUTE_TABLE_NAME_COLUMN_INDEX = 0;
    private static final int ATTRIBUTE_TABLE_TYPE_COLUMN_INDEX = 1;
    private static final int ATTRIBUTE_TABLE_ARRAY_SIZE_COLUMN_INDEX = 2;
    private static final int ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX = 3;

    private void createNewGroup()
    {
        SWTBotShell groupShell = null;

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Group").click();

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

    private void createNewDataset()
    {
        String currentSize = "4 x 4";
        SWTBotShell tableShell = null;
        SWTBotShell datasetShell = null;

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("New").menu("Dataset").click();

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
            items[0].getNode(0).getNode(0).contextMenu().contextMenu("Open").click();
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

            SWTBotMenu tableMenuItem = tableShell.bot().menu().menu("Table");
            tableMenuItem.menu("Save Changes to File").click();

            tableMenuItem.menu("Close").click();
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

    public SWTBotTableItem addAttributeToObject(String testname, SWTBotTable attrTable, int attrindex)
    {
        SWTBotShell newAttributeShell = null;
        SWTBotTableItem newItem = null;

        try {
            /* Verify that there are currently no attributes on the group */
            assertTrue(constructWrongValueMessage(testname, "attribute table wrong row count", String.valueOf(attrindex),
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == attrindex);

            SWTBotButton addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Value: ").setText(attrValue);
            newAttributeShell.bot().comboBox(2).setSelection("16");

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /* Verify that the attribute has been added to the table with the correct name and value */
            assertTrue(constructWrongValueMessage(testname, "attribute table wrong row count", String.valueOf(attrindex+1),
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == attrindex+1);

            newItem = attrTable.getTableItem(attrindex);

            assertTrue(constructWrongValueMessage(testname, "attribute wrong name", attrName,
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
            //assertTrue(constructWrongValueMessage(testname, "attribute wrong value", attrValue,
            //        newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
            //        newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));
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
        }
        return newItem;
    }

    public SWTBotTableItem addH5ScalarAttributeToObject(String testname, SWTBotTable attrTable, int attrindex)
    {
        SWTBotShell newAttributeShell = null;
        SWTBotTableItem newItem = null;

        try {
            /* Verify that there are currently no attributes on the group */
            assertTrue(constructWrongValueMessage(testname, "attribute table wrong row count", String.valueOf(attrindex),
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == attrindex);

            SWTBotButton addButton = bot.button("Add Attribute");
            addButton.click();

            newAttributeShell = bot.shell("New Attribute...");
            newAttributeShell.activate();

            newAttributeShell.bot().textWithLabel("Attribute name: ").setText(attrName);
            newAttributeShell.bot().textWithLabel("Current size").setText(attrDimsize);
            newAttributeShell.bot().comboBox(2).setSelection("16");

            newAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(newAttributeShell));

            /* Verify that the attribute has been added to the table with the correct name and value */
            assertTrue(constructWrongValueMessage(testname, "attribute table wrong row count", String.valueOf(attrindex+1),
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == attrindex+1);

            newItem = attrTable.getTableItem(attrindex);

            assertTrue(constructWrongValueMessage(testname, "attribute wrong name", attrName,
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX)),
                    newItem.getText(ATTRIBUTE_TABLE_NAME_COLUMN_INDEX).equals(attrName));
//            assertTrue(constructWrongValueMessage(testname, "attribute wrong value", attrValue,
//                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX)),
//                    newItem.getText(ATTRIBUTE_TABLE_VALUE_COLUMN_INDEX).equals(attrValue));
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
        }
        return newItem;
    }

    public void deleteAttributeFromObject(String testname, SWTBotTable attrTable, SWTBotTableItem newItem, boolean useButton)
    {
        SWTBotShell deleteAttributeShell = null;

        try {
            newItem.click();
            if (useButton)
                bot.button("Delete Attribute").click();
            else
                bot.table().contextMenu().contextMenu("Delete Attribute").click();

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
            assertTrue(constructWrongValueMessage(testname, "attribute table wrong row count", "0",
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
            if (deleteAttributeShell != null && deleteAttributeShell.isOpen()) {
                deleteAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(deleteAttributeShell));
            }
        }
    }

    public void renameAttributeFromObject(String testname, SWTBotTable attrTable, SWTBotTableItem newItem)
    {
        SWTBotShell renameAttributeShell = null;
        String newAttrName = "renamed_attribute";

        try {
            newItem.click();
            bot.table().contextMenu().contextMenu("Rename Attribute").click();

            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*" + VERSION + " - Rename Attribute");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            renameAttributeShell = bot.shell("HDFView " + VERSION + " - Rename Attribute");
            renameAttributeShell.activate();
            bot.waitUntil(Conditions.shellIsActive(renameAttributeShell.getText()));

            renameAttributeShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(renameAttributeShell));

            /*
             * Verify that the attribute has been renamed from the attribute table.
             */
            assertTrue(constructWrongValueMessage(testname, "attribute table wrong row count", "1",
                    String.valueOf(attrTable.rowCount())), attrTable.rowCount() == 1);
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
            if (renameAttributeShell != null && renameAttributeShell.isOpen()) {
                renameAttributeShell.close();
                bot.waitUntil(Conditions.shellCloses(renameAttributeShell));
            }
        }
    }

    @Test
    public void testAddHDF4Attribute()
    {
        String testFilename = "testaddattribute.hdf";
        SWTBotShell newAttributeShell = null;

        File hdf4File = createFile(testFilename);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testAddHDF4Attribute()", 1, testFilename);

            createNewGroup();
            createNewDataset();

            checkFileTree(filetree, "testAddHDF4Attribute()", 3, testFilename);
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, groupname);
            addAttributeToObject("testAddHDF4Attribute()", attrTable, 0);

            /* Now repeat the process for the dataset that was created */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            addAttributeToObject("testAddHDF4Attribute()", attrTable, 1);
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
                closeFile(hdf4File, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testAddHDF5Attribute()
    {
        String testFilename = "testaddattribute.h5";
        File hdfFile = createFile(testFilename);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testAddHDF5Attribute()", 1, testFilename);

            createNewGroup();
            createNewDataset();

            checkFileTree(filetree, "testAddHDF5Attribute()", 3, testFilename);

            /* Add an attribute to the newly-created group */
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, groupname);
            addH5ScalarAttributeToObject("testAddHDF5Attribute()", attrTable, 0);

            /* Now repeat the process for the dataset that was created */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            addH5ScalarAttributeToObject("testAddHDF5Attribute()", attrTable, 0);
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

    @Test
    public void testHDF5DeleteAttribute()
    {
        String testFilename = "testdeleteattribute.h5";

        File hdfFile = createFile(testFilename);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testHDF5DeleteAttribute()", 1, testFilename);

            createNewGroup();
            createNewDataset();

            checkFileTree(filetree, "testHDF5DeleteAttribute()", 3, testFilename);

            /* Add an attribute to the newly-created group */
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, groupname);
            SWTBotTableItem newItem = addH5ScalarAttributeToObject("testHDF5DeleteAttribute()", attrTable, 0);

            /* Test deletion of attribute by button */
            /* Now attempt to delete the attribute */
            deleteAttributeFromObject("testHDF5DeleteAttribute()", attrTable, newItem, true);

            /* Now repeat the process for the dataset created previously */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            newItem = addH5ScalarAttributeToObject("testHDF5DeleteAttribute()", attrTable, 0);

            /* Now attempt to delete the attribute */
            deleteAttributeFromObject("testHDF5DeleteAttribute()", attrTable, newItem, true);

            /* Now test deletion of attributes by using the popup menu */

            /* Reload the file for good measure */
            SWTBotTreeItem[] items = filetree.getAllItems();
            items[0].click();
            items[0].contextMenu().menu("&Reload File As").menu("Read/Write").click();

            filetree = bot.tree();
            items = filetree.getAllItems();
            items[0].contextMenu().contextMenu("Expand All").click();
            checkFileTree(filetree, "testHDF5DeleteAttribute()", 3, testFilename);
            attrTable = openAttributeTable(filetree, testFilename, groupname);
            newItem = addH5ScalarAttributeToObject("testHDF5DeleteAttribute()", attrTable, 0);

            /* Test deletion of attribute by context menu */
            /* Now attempt to delete the attribute */
            deleteAttributeFromObject("testHDF5DeleteAttribute()", attrTable, newItem, false);

            /* Now repeat the process for the dataset created previously */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            newItem = addH5ScalarAttributeToObject("testHDF5DeleteAttribute()", attrTable, 0);

            /* Now attempt to delete the attribute */
            deleteAttributeFromObject("testHDF5DeleteAttribute()", attrTable, newItem, false);
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

    @Test
    public void testHDF5RenameAttribute()
    {
        String testFilename = "testrenameattribute.h5";

        File hdfFile = createFile(testFilename);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testHDF5RenameAttribute()", 1, testFilename);

            createNewGroup();
            createNewDataset();

            checkFileTree(filetree, "testHDF5RenameAttribute()", 3, testFilename);

            /* Add an attribute to the newly-created group */
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, groupname);
            SWTBotTableItem newItem = addH5ScalarAttributeToObject("testHDF5RenameAttribute()", attrTable, 0);

            /* Now test rename of attributes by using the popup menu */

            /* Test rename of attribute by context menu */
            /* Now attempt to delete the attribute */
            renameAttributeFromObject("testHDF5RenameAttribute()", attrTable, newItem);

            /* Now repeat the process for the dataset created previously */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            newItem = addH5ScalarAttributeToObject("testHDF5RenameAttribute()", attrTable, 0);

            /* Now attempt to rename the attribute */
            renameAttributeFromObject("testHDF5RenameAttribute()", attrTable, newItem);
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

    @Test
    public void testOpenHDF4ScalarAttribute()
    {
        String testFilename = "testopenattribute.hdf";
        SWTBotShell tableShell = null;
        File hdfFile = createFile(testFilename);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testOpenHDF4ScalarAttribute()", 1, testFilename);

            createNewGroup();
            createNewDataset();

            /* Add an attribute to the newly-created group */
            checkFileTree(filetree, "testOpenHDF4ScalarAttribute()", 3, testFilename);
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, groupname);
            addAttributeToObject("testOpenHDF4ScalarAttribute()", attrTable, 0);

            attrTable = openAttributeTable(filetree, testFilename, groupname);
            tableShell = openAttributeObject(attrTable, attrName, 0);
            tableShell.close();

            /* Now repeat the process for the dataset that was created */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            addAttributeToObject("testOpenHDF4ScalarAttribute()", attrTable, 1);

            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            tableShell = openAttributeObject(attrTable, attrName, 1);
            tableShell.close();

            /* Test open of attribute by popup menu */

            /* Reload file for good measure */
            SWTBotTreeItem[] items = filetree.getAllItems();
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            items[0].contextMenu().contextMenu("Expand All").click();

            attrTable = openAttributeTable(filetree, testFilename, groupname);
            tableShell = openAttributeContext(attrTable, attrName, 0);
            tableShell.close();

            /* Now repeat the process for the previously-created dataset */
            /* However the dataset has dimension coords */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname + " (dimension)");
            tableShell = openAttributeContext(attrTable, attrName, 1);
            tableShell.close();
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
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.close();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdfFile, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testOpenHDF5ScalarAttribute()
    {
        String testFilename = "testopenattribute.h5";
        SWTBotShell tableShell = null;
        File hdfFile = createFile(testFilename);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testOpenHDF5ScalarAttribute()", 1, testFilename);

            createNewGroup();
            createNewDataset();

            /* Add an attribute to the newly-created group */
            checkFileTree(filetree, "testOpenHDF5ScalarAttribute()", 3, testFilename);
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, groupname);
            addH5ScalarAttributeToObject("testOpenHDF5ScalarAttribute()", attrTable, 0);

            attrTable = openAttributeTable(filetree, testFilename, groupname);
            tableShell = openAttributeObject(attrTable, attrName, 0);
            tableShell.close();

            /* Now repeat the process for the dataset that was created */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            addH5ScalarAttributeToObject("testOpenHDF5ScalarAttribute()", attrTable, 0);

            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            tableShell = openAttributeObject(attrTable, attrName, 0);
            tableShell.close();

            /* Test open of attribute by popup menu */

            /* Reload file for good measure */
            SWTBotTreeItem[] items = filetree.getAllItems();
            items[0].click();
            items[0].contextMenu().contextMenu("Reload File").click();

            items = filetree.getAllItems();
            items[0].contextMenu().contextMenu("Expand All").click();

            attrTable = openAttributeTable(filetree, testFilename, groupname);
            tableShell = openAttributeContext(attrTable, attrName, 0);
            tableShell.close();


            /* Now repeat the process for the previously-created dataset */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            tableShell = openAttributeContext(attrTable, attrName, 0);
            tableShell.close();
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
            if (tableShell != null && tableShell.isOpen()) {
                tableShell.close();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdfFile, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

//    @Ignore
//    public void testOpenHDF5CompoundAttribute() {
//        try {
//            /* Test open of attribute by double-click */
//
//            /* Test open of attribute by popup menu */
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//            fail(ex.getMessage());
//        }
//        catch (AssertionError ae) {
//            ae.printStackTrace();
//            fail(ae.getMessage());
//        }
//    }

//    @Ignore
//    public void testEditHDF4ScalarAttribute() {
//        try {
//
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//            fail(ex.getMessage());
//        }
//        catch (AssertionError ae) {
//            ae.printStackTrace();
//            fail(ae.getMessage());
//        }
//    }

//    @Ignore
//    public void testEditHDF5ScalarAttribute() {
//        try {
//
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//            fail(ex.getMessage());
//        }
//        catch (AssertionError ae) {
//            ae.printStackTrace();
//            fail(ae.getMessage());
//        }
//    }

//    @Ignore
//    public void testEditHDF5CompoundAttribute() {
//        try {
//
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//            fail(ex.getMessage());
//        }
//        catch (AssertionError ae) {
//            ae.printStackTrace();
//            fail(ae.getMessage());
//        }
//    }

//    @Ignore
//    public void testDiscardHDF4ScalarAttributeEditResults() {
//        try {
//
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//            fail(ex.getMessage());
//        }
//        catch (AssertionError ae) {
//            ae.printStackTrace();
//            fail(ae.getMessage());
//        }
//    }

//    @Ignore
//    public void testDiscardHDF5ScalarAttributeEditResults() {
//        try {
//
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//            fail(ex.getMessage());
//        }
//        catch (AssertionError ae) {
//            ae.printStackTrace();
//            fail(ae.getMessage());
//        }
//    }

//    @Ignore
//    public void testDiscardHDF5CompoundAttributeEditResults() {
//        try {
//
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//            fail(ex.getMessage());
//        }
//        catch (AssertionError ae) {
//            ae.printStackTrace();
//            fail(ae.getMessage());
//        }
//    }

    @Test
    public void testHDF4RenameAttributeFunctionDisabled() {
        String testFilename = "testrenameattributedisabled.hdf";
        File hdfFile = createFile(testFilename);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testHDF4RenameAttributeFunctionDisabled()", 1, testFilename);

            createNewGroup();
            createNewDataset();

            checkFileTree(filetree, "testHDF4RenameAttributeFunctionDisabled()", 3, testFilename);

            /* Add an attribute to the newly-created group */
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, groupname);
            SWTBotTableItem newItem = addAttributeToObject("testHDF4RenameAttributeFunctionDisabled()", attrTable, 0);

            newItem.click();

            assertTrue(
                    constructWrongValueMessage("testHDF4RenameAttributeFunctionDisabled()",
                            "rename attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu().contextMenu("Rename Attribute").isEnabled());

            /* Now repeat the process for the dataset that was created */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            newItem = addAttributeToObject("testHDF4RenameAttributeFunctionDisabled()", attrTable, 1);

            newItem.click();

            assertTrue(
                    constructWrongValueMessage("testHDF4RenameAttributeFunctionDisabled()",
                            "rename attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu().contextMenu("Rename Attribute").isEnabled());
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

    @Test
    public void testHDF4DeleteAttributeFunctionDisabled() {
        String testFilename = "testdeleteattributedisabled.hdf";
        File hdfFile = createFile(testFilename);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testHDF4DeleteAttributeFunctionDisabled()", 1, testFilename);

            createNewGroup();
            createNewDataset();

            checkFileTree(filetree, "testHDF4DeleteAttributeFunctionDisabled()", 3, testFilename);

            /* Add an attribute to the newly-created group */
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, groupname);
            SWTBotTableItem newItem = addAttributeToObject("testHDF4DeleteAttributeFunctionDisabled()", attrTable, 0);

            newItem.click();

            assertTrue(
                    constructWrongValueMessage("testHDF4DeleteAttributeFunctionDisabled()",
                            "delete attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu().contextMenu("Delete Attribute").isEnabled());
            assertTrue(
                    constructWrongValueMessage("testHDF4DeleteAttributeFunctionDisabled()",
                            "delete attribute button not disabled", "disabled", "enabled"),
                    !bot.button("Delete Attribute").isEnabled());

            /* Now repeat the process for the dataset that was created */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            newItem = addAttributeToObject("testHDF4RenameAttributeFunctionDisabled()", attrTable, 1);

            newItem.click();

            assertTrue(
                    constructWrongValueMessage("testHDF4DeleteAttributeFunctionDisabled()",
                            "delete attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu().contextMenu("Delete Attribute").isEnabled());
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
                closeFile(hdfFile, true);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

//    @Ignore
//    public void testHDF4AttributeEditDisabledForReadOnly() {
//        try {
//
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//            fail(ex.getMessage());
//        }
//        catch (AssertionError ae) {
//            ae.printStackTrace();
//            fail(ae.getMessage());
//        }
//    }

    @Test
    public void testHDF5RenameAttributeDisabledForReadOnly() {
        String testFilename = "testrenameattributedisabledreadonly.h5";
        File hdfFile = createFile(testFilename);

        try {
            SWTBotTree filetree = bot.tree();
            checkFileTree(filetree, "testHDF5RenameAttributeDisabledForReadOnly()", 1, testFilename);

            createNewGroup();
            createNewDataset();

            checkFileTree(filetree, "testHDF5RenameAttributeDisabledForReadOnly()", 3, testFilename);
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, groupname);
            addH5ScalarAttributeToObject("testHDF5RenameAttributeDisabledForReadOnly()", attrTable, 0);

            /* Now repeat the process for the dataset that was created */
            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);
            SWTBotTableItem newItem = addH5ScalarAttributeToObject("testHDF5RenameAttributeDisabledForReadOnly()", attrTable, 0);
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
                closeFile(hdfFile, false);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        hdfFile = openFile(testFilename, FILE_MODE.READ_ONLY);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();
            items[0].contextMenu().contextMenu("Expand All").click();
            checkFileTree(filetree, "testHDF5RenameAttributeDisabledForReadOnly()", 3, testFilename);
            SWTBotTable attrTable = openAttributeTable(filetree, testFilename, groupname);

            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttributeDisabledForReadOnly()",
                            "rename attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu().contextMenu("Rename Attribute").isEnabled());

            attrTable = openAttributeTable(filetree, testFilename, groupname + '/' + datasetname);

            assertTrue(
                    constructWrongValueMessage("testHDF5RenameAttributeDisabledForReadOnly()",
                            "rename attribute menuitem not disabled", "disabled", "enabled"),
                    !bot.table().contextMenu().contextMenu("Rename Attribute").isEnabled());
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

//    @Ignore
//    public void testHDF5AttributeEditDisabledForReadOnly() {
//        try {
//
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//            fail(ex.getMessage());
//        }
//        catch (AssertionError ae) {
//            ae.printStackTrace();
//            fail(ae.getMessage());
//        }
//    }

}
