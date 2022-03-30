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
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

public class TestTreeViewNewMenu extends AbstractWindowTest {
    @Test
    public void createNewHDF5Dataset() {
        String filename = "testds.h5";
        String groupname = "testgroupname";
        String datasetname = "testdatasetname";
        String datasetdimsize = "4 x 4";
        SWTBotShell tableShell = null;
        File hdf_file = createFile(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createNewHDF5Dataset() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);

            items[0].click();
            items[0].contextMenu().contextMenu("New").menu("Group").click();

            SWTBotShell groupShell = bot.shell("New Group...");
            groupShell.activate();
            bot.waitUntil(Conditions.shellIsActive(groupShell.getText()));

            groupShell.bot().text(0).setText(groupname);

            String val = groupShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "wrong group name", groupname, val),
                    val.equals(groupname));

            groupShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(groupShell));

            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("createNewHDF5Dataset() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("createNewHDF5Dataset() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname)==0);

            items[0].getNode(0).click();

            items[0].getNode(0).contextMenu().contextMenu("New").menu("Dataset").click();

            SWTBotShell datasetShell = bot.shell("New Dataset...");
            datasetShell.activate();
            bot.waitUntil(Conditions.shellIsActive(datasetShell.getText()));

            datasetShell.bot().text(0).setText(datasetname);
            datasetShell.bot().text(2).setText(datasetdimsize);

            val = datasetShell.bot().text(0).getText();
            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "wrong dataset name", datasetname, val),
                    val.equals(datasetname));

            val = datasetShell.bot().text(2).getText();
            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "wrong dataset dimension sizes", datasetdimsize, val),
                    val.equals(datasetdimsize));

            datasetShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(datasetShell));

            items[0].getNode(0).click();
            items[0].getNode(0).contextMenu().contextMenu("Expand All").click();

            assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "filetree wrong row count", "3", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==3);
            assertTrue("createNewHDF5Dataset() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("createNewHDF5Dataset() filetree is missing group '" + groupname + "'", items[0].getNode(0).getText().compareTo(groupname)==0);
            assertTrue("createNewHDF5Dataset() filetree is missing dataset '" + datasetname + "'", items[0].getNode(0).getNode(0).getText().compareTo(datasetname)==0);

            items[0].getNode(0).getNode(0).click();
            items[0].getNode(0).getNode(0).contextMenu().contextMenu("Open").click();
            org.hamcrest.Matcher<Shell> shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            final SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            for (int row = 1; row <= table.preferredRowCount() - 1; row++) {
                for (int col = 1; col <= table.preferredColumnCount() - 1; col++) {
                    final String thisVal = String.valueOf(((row - 1) * (table.preferredColumnCount() - 1)) + (col));

                    // Note: setCellDataValueByPosition throws a null pointer exception in SWTBot currently,
                    // resort to manual workaround below
                    // table.setCellDataValueByPosition(row, col, val);

                    table.doubleclick(row, col);

                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            table.widget.getActiveCellEditor().setEditorValue(thisVal);
                            table.widget.getActiveCellEditor().commit(SelectionLayer.MoveDirectionEnum.RIGHT, true, true);
                        }
                    });
                }
            }

            tableShell.bot().menu().menu("Table").menu("Save Changes to File").click();

            tableShell.bot().menu().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));

            items[0].getNode(0).getNode(0).click();
            items[0].getNode(0).getNode(0).contextMenu().contextMenu("Open").click();
            shellMatcher = WithRegex.withRegex(".*at.*\\[.*in.*\\]");
            bot.waitUntil(Conditions.waitForShell(shellMatcher));

            tableShell = bot.shells()[1];
            tableShell.activate();
            bot.waitUntil(Conditions.shellIsActive(tableShell.getText()));

            SWTBotNatTable table2 = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));

            for (int row = 1; row <= table2.preferredRowCount() - 1; row++) {
                for (int col = 1; col < table2.preferredColumnCount(); col++) {
                    String expected = String.valueOf(((row - 1) * (table2.preferredColumnCount() - 1)) + (col));
                    val = table2.getCellDataValueByPosition(row, col);
                    assertTrue(constructWrongValueMessage("createNewHDF5Dataset()", "wrong data", expected, val), val.equals(expected));
                }
            }

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
