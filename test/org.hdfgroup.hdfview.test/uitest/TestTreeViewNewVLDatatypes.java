package uitest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

public class TestTreeViewNewVLDatatypes extends AbstractWindowTest {
    @Test
    public void createNewHDF5VLDatatype() {
        String filename = "testvldt.h5";
        String dtname = "testvldtname";
        SWTBotShell tableShell = null;
        File hdf_file = createFile(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLDatatype()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createNewHDF5VLDatatype() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);

            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Datatype").click();

            SWTBotShell dtShell = bot.shell("New Datatype...");
            dtShell.activate();
            bot.waitUntil(Conditions.shellIsActive(dtShell.getText()));

            dtShell.bot().text(0).setText(dtname);

            dtShell.bot().comboBox(2).setSelection("VLEN_INTEGER");
            dtShell.bot().comboBox(3).setSelection("16");

            dtShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(dtShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLDatatype()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("createNewHDF5VLDatatype() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("createNewHDF5VLDatatype() filetree is missing group '" + dtname + "'", items[0].getNode(0).getText().compareTo(dtname)==0);
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }

    @Test
    public void createNewHDF5VLDataset() {
        String filename = "testvldataset.h5";
        String dsname = "testvldatasetname";
        SWTBotShell tableShell = null;
        File hdf_file = createFile(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLDataset()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createNewHDF5VLDataset() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);

            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Dataset").click();

            SWTBotShell dsShell = bot.shell("New Dataset...");
            dsShell.activate();
            bot.waitUntil(Conditions.shellIsActive(dsShell.getText()));

            dsShell.bot().text(0).setText(dsname);

            dsShell.bot().comboBox(2).setSelection("VLEN_FLOAT");
            dsShell.bot().comboBox(3).setSelection("32");

            dsShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(dsShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLDataset()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("createNewHDF5VLDataset() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("createNewHDF5VLDataset() filetree is missing dataset '" + dsname + "'", items[0].getNode(0).getText().compareTo(dsname)==0);

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(dsname + ".*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            table.click(1, 1);
            String initval = tableShell.bot().text(0).getText();

            String expected = "[]";
            assertTrue(constructWrongValueMessage("createNewHDF5VLDataset()", "wrong data", expected, initval), initval.equals(expected));

            final SWTBotNatTable edittable = table;
            final SWTBotShell editShell = tableShell;
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    String val = "[1.0, 2.1, 3.2]";
                    edittable.doubleclick(1, 1);
                    edittable.widget.getActiveCellEditor().setEditorValue(val);
                    edittable.widget.getActiveCellEditor().commit(SelectionLayer.MoveDirectionEnum.RIGHT, true, true);
                    edittable.click(1, 1);
                    String newval = editShell.bot().text(0).getText();
                    assertTrue(constructWrongValueMessage("createNewHDF5VLDataset()", "wrong value",
                            val, newval), newval.equals(val));
                }
            });

            tableShell.bot().menu().menu("Table").menu("Save Changes to File").click();

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table2 = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            String expected2 = "[1.0, 2.1, 3.2]";
            table2.click(1, 1);
            String updateval = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("createNewHDF5VLDataset()", "wrong data", expected2, updateval), updateval.equals(expected2));

            tableShell.bot().menu().menu("Table").menu("Close").click();
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
                tableShell.bot().menu().menu("Table").menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }

    @Test
    public void createNewHDF5VLAttribute() {
        String filename = "testvlattr.h5";
        String daname = "testvlattrname";
        SWTBotShell tableShell = null;
        File hdf_file = createFile(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLAttribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createNewHDF5VLAttribute() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);

            items[0].click();

            SWTBotTabItem tabItem = bot.tabItem("Object Attribute Info");
            tabItem.activate();

            bot.button("Add Attribute").click();

            SWTBotShell daShell = bot.shell("New Attribute...");
            daShell.activate();
            bot.waitUntil(Conditions.shellIsActive(daShell.getText()));

            daShell.bot().text(0).setText(daname);

            daShell.bot().comboBox(1).setSelection("VLEN_INTEGER");

            daShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(daShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLAttribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createNewHDF5VLAttribute() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);

            // Open dataset Attribute Table
            SWTBotTable attrTable = openAttributeTable(filetree, filename, "/");
            tableShell = openAttributeObject(attrTable, daname, 0);

            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            table.click(1, 1);
            String initval = tableShell.bot().text(0).getText();

            String expected = "[]";
            assertTrue(constructWrongValueMessage("createNewHDF5VLAttribute()", "wrong data", expected, initval), initval.equals(expected));

            final SWTBotNatTable edittable = table;
            final SWTBotShell editShell = tableShell;
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    String val = "[1, 2, 3]";
                    edittable.doubleclick(1, 1);
                    edittable.widget.getActiveCellEditor().setEditorValue(val);
                    edittable.widget.getActiveCellEditor().commit(SelectionLayer.MoveDirectionEnum.RIGHT, true, true);
                    edittable.click(1, 1);
                    String newval = editShell.bot().text(0).getText();
                    assertTrue(constructWrongValueMessage("createNewHDF5VLAttribute()", "wrong value",
                            val, newval), newval.equals(val));
                }
            });

            tableShell.bot().menu().menu("Table").menu("Save Changes to File").click();

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].click();

            // Open dataset Attribute Table
            attrTable = openAttributeTable(filetree, filename, "/");
            tableShell = openAttributeObject(attrTable, daname, 0);

            SWTBotNatTable table2 = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            String expected2 = "[1, 2, 3]";
            table2.click(1, 1);
            String updateval = tableShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("createNewHDF5VLDataset()", "wrong data", expected2, updateval), updateval.equals(expected2));

            tableShell.bot().menu().menu("Table").menu("Close").click();
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
                tableShell.bot().menu().menu("Table").menu("Close").click();
                bot.waitUntil(Conditions.shellCloses(tableShell));
            }

            try {
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }
}
