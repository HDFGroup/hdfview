package test.uitest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;

import java.io.File;

import javax.swing.KeyStroke;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.nebula.nattable.finder.widgets.SWTBotNatTable;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.matchers.WithRegex;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Test;

public class TestTreeViewNewMenu extends AbstractWindowTest {
    @Test
    public void createNewHDF5Dataset() {
        String filename = "testds";
        String file_ext = ".h5";
        String groupname = "testgroupname";
        String datasetname = "testdatasetname";
        String datasetdimsize = "4 x 4";
        File hdf_file = createHDF5File(filename);

        try {
            SWTBotTree filetree = bot.tree();
            SWTBotTreeItem[] items = filetree.getAllItems();

            assertTrue("File-Dataset-HDF5 filetree row count: "+filetree.visibleRowCount(), filetree.visibleRowCount()==1);
            assertTrue("File-Dataset-HDF5 filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext)==0);
            
            items[0].click();
            
            filetree.contextMenu("New").menu("Group").click();
            
            SWTBotShell groupShell = bot.shell("New Group...");
            groupShell.activate();
            bot.waitUntil(Conditions.shellIsActive(groupShell.getText()));
            
            groupShell.bot().text(0).setText(groupname);
            assertEquals(groupShell.bot().text(0).getText(), groupname);
            
            groupShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(groupShell));
            
            assertTrue("File-Dataset-HDF5 filetree row count: "+filetree.visibleRowCount(), filetree.visibleRowCount()==2);
            assertTrue("File-Dataset-HDF5 filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("File-Dataset-HDF5 filetree is missing group " + groupname, items[0].getNode(0).getText().compareTo(groupname)==0);
            
            items[0].getNode(0).click();
            
            filetree.contextMenu("New").menu("Dataset").click();
            
            SWTBotShell datasetShell = bot.shell("New Dataset...");
            datasetShell.activate();
            bot.waitUntil(Conditions.shellIsActive(datasetShell.getText()));
            
            datasetShell.bot().text(0).setText(datasetname);
            datasetShell.bot().text(2).setText(datasetdimsize);
            assertEquals(datasetShell.bot().text(0).getText(), datasetname);
            assertEquals(datasetShell.bot().text(2).getText(), datasetdimsize);
            
            datasetShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(datasetShell));
            
            items[0].getNode(0).click();
            filetree.contextMenu("Expand All").click();
            
            assertTrue("File-Dataset-HDF5 filetree row count: "+filetree.visibleRowCount(), filetree.visibleRowCount()==3);
            assertTrue("File-Dataset-HDF5 filetree is missing file " + filename + file_ext, items[0].getText().compareTo(filename + file_ext)==0);
            assertTrue("File-Dataset-HDF5 filetree is missing group " + groupname, items[0].getNode(0).getText().compareTo(groupname)==0);
            assertTrue("File-Dataset-HDF5 filetree is missing dataset " + datasetname, items[0].getNode(0).getNode(0).getText().compareTo(datasetname)==0);
            
            items[0].getNode(0).getNode(0).click();
            
            filetree.contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(".*at.*\\[.*in.*\\]")));
            
            SWTBotShell tableShell = bot.shells()[1];
            SWTBotNatTable table = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));
            
            for (int row = 1; row <= table.preferredRowCount() - 1; row++) {
                for (int col = 1; col < table.preferredColumnCount(); col++) {
                    table.doubleclick(row, col);
                    bot.waitUntil(Conditions.waitForWidget(WidgetOfType.widgetOfType(org.eclipse.swt.widgets.Text.class)));
                    tableShell.bot().text("0", 1).setText(String.valueOf(((row - 1) * (table.preferredColumnCount() - 1)) + (col)));
                    
                    // Press enter to set value
                    table.pressShortcut(SWT.NONE, SWT.CR, ' ');
                }
            }
            
//            Display.getDefault().syncExec(new Runnable() {
//                public void run() {
//                    for (int row = 1; row <= table.preferredRowCount() - 1; row++) {
//                        for (int col = 1; col < table.preferredColumnCount(); col++) {
//                            int rowIndex = table.widget.getRowIndexByPosition(row);
//                            int colIndex = table.widget.getColumnIndexByPosition(col);
//                            table.setCellDataValueByPosition(rowIndex, colIndex, String.valueOf(((row - 1) * table.preferredColumnCount()) + col));
//                        }
//                    }
//                }
//            });
            
            tableShell.bot().menu("Table").menu("Save Changes to File").click();
            
            tableShell.bot().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
            
            items[0].getNode(0).getNode(0).click();
            
            filetree.contextMenu("Open").click();
            bot.waitUntil(Conditions.waitForShell(WithRegex.withRegex(".*at.*\\[.*in.*\\]")));
            
            tableShell = bot.shells()[1];
            SWTBotNatTable table2 = new SWTBotNatTable(tableShell.bot().widget(WidgetOfType.widgetOfType(NatTable.class)));
            
            for (int row = 1; row <= table2.preferredRowCount() - 1; row++) {
                for (int col = 1; col < table2.preferredColumnCount(); col++) {
                    assertEquals(table2.getCellDataValueByPosition(row, col), String.valueOf(((row - 1) * (table2.preferredColumnCount() - 1)) + (col)));
                }
            }
            
            tableShell.bot().menu("Table").menu("Close").click();
            bot.waitUntil(Conditions.shellCloses(tableShell));
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
            catch (Exception ex) {}
        }
    }
}
