package test.uitest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTabItem;
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
            items[0].contextMenu("New").menu("Datatype").click();

            SWTBotShell dtShell = bot.shell("New Datatype...");
            dtShell.activate();
            bot.waitUntil(Conditions.shellIsActive(dtShell.getText()));

            dtShell.bot().text(0).setText(dtname);

            dtShell.bot().comboBox(1).setSelection("VLEN_INTEGER");
            dtShell.bot().comboBox(2).setSelection("16");

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
                tableShell.bot().menu("Close").click();
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
            items[0].contextMenu("New").menu("Dataset").click();

            SWTBotShell dsShell = bot.shell("New Dataset...");
            dsShell.activate();
            bot.waitUntil(Conditions.shellIsActive(dsShell.getText()));

            dsShell.bot().text(0).setText(dsname);

            dsShell.bot().comboBox(1).setSelection("VLEN_FLOAT");
            dsShell.bot().comboBox(2).setSelection("32");

            dsShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(dsShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLDataset()", "filetree wrong row count", "2", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==2);
            assertTrue("createNewHDF5VLDataset() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
            assertTrue("createNewHDF5VLDataset() filetree is missing dataset '" + dsname + "'", items[0].getNode(0).getText().compareTo(dsname)==0);
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

            daShell.bot().comboBox(0).setSelection("VLEN_STRING");
            daShell.bot().text(2).setText("ABC");

            daShell.bot().button("   &OK   ").click();
            bot.waitUntil(Conditions.shellCloses(daShell));

            items = filetree.getAllItems();

            assertTrue(constructWrongValueMessage("createNewHDF5VLAttribute()", "filetree wrong row count", "1", String.valueOf(filetree.visibleRowCount())),
                    filetree.visibleRowCount()==1);
            assertTrue("createNewHDF5VLAttribute() filetree is missing file '" + filename + "'", items[0].getText().compareTo(filename)==0);
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
                closeFile(hdf_file, true);
            }
            catch (Exception ex) {}
        }
    }
}
